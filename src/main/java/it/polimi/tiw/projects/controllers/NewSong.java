package it.polimi.tiw.projects.controllers;

import it.polimi.tiw.projects.beans.Album;
import it.polimi.tiw.projects.beans.User;
import it.polimi.tiw.projects.dao.AlbumDAO;
import it.polimi.tiw.projects.dao.AudioDAO;
import it.polimi.tiw.projects.dao.ImageDAO;
import it.polimi.tiw.projects.dao.SongDAO;
import it.polimi.tiw.projects.exceptions.DAOException;
import it.polimi.tiw.projects.listeners.AppContextListener;
import it.polimi.tiw.projects.utils.ConnectionHandler;
import it.polimi.tiw.projects.utils.Genre;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;
import jakarta.validation.constraints.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

@MultipartConfig
public class NewSong extends HttpServlet {
    private static final Logger logger = LoggerFactory.getLogger(NewSong.class);
    private static final long serialVersionUID = 1L;
    private Connection connection = null;

    public NewSong() {
        super();
    }

    private static Album findAlbum(List<Album> list, String albumName) {
        return list.stream().filter(a -> a.getName().equalsIgnoreCase(albumName)).findFirst().orElse(null);
    }

    private static String areParametersOk(HttpServletRequest req, ServletContext servletContext) {

        Pattern titlePattern = (Pattern) servletContext.getAttribute(AppContextListener.TITLE_REGEX_PATTERN);

        try {
            String title = req.getParameter("sTitle");
            if (title == null || (title = title.strip()).isEmpty()) {
                return "You have to choose a title";
            }
            if (isValid(title, titlePattern)) {
                return "Invalid title format. Use letters, numbers, spaces, hyphens, or apostrophes (1-100 characters).";
            }

            String albumName = req.getParameter("sAlbum");
            if (albumName == null || (albumName = albumName.strip()).isEmpty()) {
                return "You have to specify the album name";
            }
            if (isValid(albumName, titlePattern)) {
                return "Invalid album name format. Use letters, numbers, spaces, hyphens, or apostrophes (1-100 characters).";
            }

            String yearString = req.getParameter("sYear");
            if (yearString == null || (yearString = yearString.strip()).isEmpty()) {
                return "You have to specify the album's year of release";
            }
            try {
                Integer.parseInt(yearString);
            } catch (NumberFormatException e) {
                return "The year must be a valid number";
            }

            String artist = req.getParameter("sArtist");
            if (artist == null || (artist = artist.strip()).isEmpty()) {
                return "You have to specify the name of the artist";
            }
            if (isValid(artist, titlePattern)) {
                return "Invalid artist name format. Use letters, numbers, spaces, hyphens, or apostrophes (1-100 characters).";
            }

            String genreName = req.getParameter("sGenre");
            if (genreName == null || (genreName = genreName.strip()).isEmpty()) {
                return "You must choose a genre from the predefined ones";
            }
            try {
                Genre.valueOf(genreName);
            } catch (IllegalArgumentException e) {
                return "You must choose a genre from the predefined ones";
            }

            Part imagePart = req.getPart("sIcon");
            if (imagePart == null || imagePart.getSize() == 0) {
                return "You must upload an image";
            }
            String imageType = imagePart.getContentType();
            if (imageType == null || !imageType.startsWith("image/")) {
                return "The uploaded file must be a valid image";
            }

            Part audioPart = req.getPart("sFile");
            if (audioPart == null || audioPart.getSize() == 0) {
                return "You must upload an audio file";
            }
            String audioType = audioPart.getContentType();
            if (audioType == null || !audioType.startsWith("audio/")) {
                return "The uploaded file must be a valid audio file";
            }

        } catch (IOException | ServletException e) {
            return "Error while processing form data";
        }

        return null; // all good
    }

    private static boolean isValid(@NotNull String parameter, @NotNull Pattern pattern) {
        return !pattern.matcher(parameter).matches();
    }

    @Override
    public void init() throws ServletException {
        ServletContext context = getServletContext();
        connection = ConnectionHandler.getConnection(context);
    }

    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
        logger.debug("processing the Post request");
        SongDAO songDAO = new SongDAO(connection);
        AlbumDAO albumDAO = new AlbumDAO(connection);
        AudioDAO audioDAO = (AudioDAO) getServletContext().getAttribute("audioDAO");
        ImageDAO imageDAO = (ImageDAO) getServletContext().getAttribute("imageDAO");
        UUID userId = ((User) req.getSession().getAttribute("user")).getIdUser();

        // Check Parameters
        String checkResult = areParametersOk(req, getServletContext());

        if (checkResult != null) {
            req.setAttribute("errorNewSongMsg", checkResult);
            logger.warn("ParametersNotOk: " + checkResult);
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            req.getRequestDispatcher("/Home").forward(req, resp);
            return;
        }

        logger.info("Parameters are ok");

        // Retrieve parameters
        String title = req.getParameter("sTitle").strip();
        String albumName = req.getParameter("sAlbum").strip();
        Integer year = Integer.valueOf(req.getParameter("sYear").strip());
        String artist = req.getParameter("sArtist").strip();
        Genre genre;
        try {
            genre = Genre.valueOf((req.getParameter("sGenre").strip()));
        } catch (IllegalArgumentException | NullPointerException e) {
            req.setAttribute("errorNewSongMsg", "You must choose a genre from the predefined ones");
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            req.getRequestDispatcher("/Home").forward(req, resp);
            return;
        }
        Part imagePart = req.getPart("sIcon");
        Part audioPart = req.getPart("sFile");

        logger.debug("Retrieved Parameters");

        // Find the list of all the albums
        List<Album> albums;
        try {
            albums = albumDAO.findAlbumsByUser(userId);
        } catch (DAOException e) {
            logger.error("Failed to retrieve the album by user: {}", e.getMessage(), e);
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error in the database");
            return;
        }

        // Search if there is already an album with that name
        Album album = findAlbum(albums, albumName);

        if (album != null && (!(album.getYear() == year) || !album.getArtist().equalsIgnoreCase(artist))) {
            // If an album with that name already exists but the information don't match
            req.setAttribute("errorNewSongMsg", "An album named \"" + album.getName()
                    + "\" already exists, it is from the year " + album.getYear() + " by \"" + album.getArtist() + "\"");
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            req.getRequestDispatcher("/Home").forward(req, resp);
            return;
        }

        // An album can't have songs with the same name
        try {
            if (album != null && songDAO.findSongsByUser(userId).stream()
                    .anyMatch(s -> s.getTitle().equals(title))) {

                req.setAttribute("errorNewSongMsg", "The song titled \"" + title + "\" of the album \""
                        + album.getName() + "\" have already been uploaded");
                resp.setStatus(HttpServletResponse.SC_CONFLICT);
                req.getRequestDispatcher("/Home").forward(req, resp);
                return;
            }
        } catch (DAOException e) {
            logger.error("Error while searching user songs: {}", e.getMessage(), e);
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error in the database");
            return;
        }

        String imageFileRename = null;
        Boolean isAlbumNew = false;

        if (album == null) {
            isAlbumNew = true;
            // If an album already exists the image will not be updated

            String imageFileName = Paths.get(imagePart.getSubmittedFileName()).getFileName().toString();

            try {
                try (InputStream imageStream = imagePart.getInputStream()) {
                    imageFileRename = imageDAO.saveImage(imageStream, imageFileName);
                    // If the image saving fails, nothing will be saved
                }
            } catch (IllegalArgumentException e) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST,
                        "The image file you provided may be corrupted");
                logger.error("Problem with the imageFile: {}", e.getMessage(), e);
                return;
            } catch (DAOException e) {
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                        "Failed to save image due to I/O error");
                logger.error("I/O error: {}", e.getMessage(), e);
                return;
            }

        }

        // Create a new album if it doesn't exist
        if (album == null) {
            try {
                album = albumDAO.createAlbum(albumName, year, artist, imageFileRename, userId);
            } catch (DAOException e) {
                // We need to delete the saved image since the album creation failed.

                try {
                    imageDAO.deleteImage(imageFileRename);
                } catch (IllegalArgumentException | DAOException e1) {
                    logger.error("Error while deleting image due to failed album creation: {}", e1.getMessage(), e1);
                    return;
                }

                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error in the database");
                logger.error("Error creating album: {}", e.getMessage(), e);
                return;
            }
        }
        int idAlbum = album.getIdAlbum();

        // Saving the audio file

        String audioFileName = Paths.get(audioPart.getSubmittedFileName()).getFileName().toString();

        String audioFileRename;
        try {
            try (InputStream audioStream = audioPart.getInputStream()) {
                audioFileRename = audioDAO.saveAudio(audioStream, audioFileName);
            }
        } catch (IllegalArgumentException e) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST,
                    "The audio file you provided may be corrupted");
            logger.error("Audio file broken: {}", e.getMessage(), e);

            // Deleting the album
            if (isAlbumNew) {
                try {
                    imageDAO.deleteImage(album.getImage());
                    albumDAO.deleteAlbum(idAlbum, userId);
                } catch (DAOException e1) {
                    logger.error(
                            "While trying to delete an album because of the failed creation of the song an error occurred: {}",
                            e1.getMessage(), e1);
                }
            }
            return;
        } catch (DAOException e) {
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to save image due to I/O error");
            logger.error("Error saving the audio: {}", e.getMessage(), e);
            // Deleting the album
            if (isAlbumNew) {
                try {
                    imageDAO.deleteImage(album.getImage());
                    albumDAO.deleteAlbum(idAlbum, userId);
                } catch (DAOException e1) {
                    logger.error(
                            "While trying to delete an album because of the failed creation of the song an error occurred: {}",
                            e1.getMessage(), e1);
                }
            }
            return;
        }

        try {
            songDAO.createSong(title, idAlbum, genre, audioFileRename, userId);
        } catch (DAOException e) {
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error in the database");
            logger.error("An error occurred while creating the song: {}", e.getMessage(), e);
            // Deleting the audio

            try {
                audioDAO.deleteAudio(audioFileRename);
            } catch (IllegalArgumentException | DAOException e1) {
                logger.error("Error while deleting audio due to failed song creation: {}", e.getMessage(), e);
                return;
            }

            // Deleting the album
            if (isAlbumNew) {
                try {
                    imageDAO.deleteImage(album.getImage());
                    albumDAO.deleteAlbum(idAlbum, userId);
                } catch (DAOException e1) {
                    logger.error(
                            "While trying to delete an album because of the failed creation of the song an error occurred: {}",
                            e1.getMessage(), e1);
                }
            }
            return;
        }


        String path = getServletContext().getContextPath() + "/Home";
        resp.sendRedirect(path);

    }

    @Override
    public void destroy() {
        try {
            ConnectionHandler.closeConnection(connection);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
