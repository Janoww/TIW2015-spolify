package it.polimi.tiw.projects.controllers.api;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import it.polimi.tiw.projects.beans.Album;
import it.polimi.tiw.projects.beans.FileData;
import it.polimi.tiw.projects.beans.Song;
import it.polimi.tiw.projects.beans.SongWithAlbum;
import it.polimi.tiw.projects.beans.User;
import it.polimi.tiw.projects.dao.AlbumDAO;
import it.polimi.tiw.projects.dao.AudioDAO;
import it.polimi.tiw.projects.dao.ImageDAO;
import it.polimi.tiw.projects.dao.SongDAO;
import it.polimi.tiw.projects.exceptions.DAOException;
import it.polimi.tiw.projects.utils.ConnectionHandler;
import it.polimi.tiw.projects.utils.Genre;
import it.polimi.tiw.projects.utils.ResponseUtils;

import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.UnavailableException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.Part;

@WebServlet("/api/v1/songs/*")
@MultipartConfig
public class SongApiServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final Logger logger = LoggerFactory.getLogger(SongApiServlet.class);

    private Connection connection;
    private ObjectMapper objectMapper;
    private SongDAO songDAO;
    private AlbumDAO albumDAO;
    private ImageDAO imageDAO;
    private AudioDAO audioDAO;

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        ServletContext servletContext = config.getServletContext();
        try {
            this.connection = ConnectionHandler.getConnection(servletContext);
        } catch (UnavailableException e) {
            logger.error("Database connection unavailable during servlet init.", e);
            throw new ServletException("Database connection unavailable", e);
        }

        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        this.songDAO = new SongDAO(connection);
        this.albumDAO = new AlbumDAO(connection);

        this.imageDAO = (ImageDAO) servletContext.getAttribute("imageDAO");
        this.audioDAO = (AudioDAO) servletContext.getAttribute("audioDAO");

        if (this.imageDAO == null || this.audioDAO == null) {
            logger.error(
                    "ImageDAO or AudioDAO not found in ServletContext. Check AppContextListener.");
            throw new ServletException(
                    "Critical file DAOs not initialized. Check AppContextListener setup.");
        }

        logger.info("SongApiServlet initialized successfully.");
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        HttpSession session = request.getSession(false);
        User user = (session != null) ? (User) session.getAttribute("user") : null;

        if (user == null) {
            ResponseUtils.sendError(response, HttpServletResponse.SC_UNAUTHORIZED,
                    "User not authenticated.");
            return;
        }

        String pathInfo = request.getPathInfo();

        try {
            if (pathInfo == null || pathInfo.equals("/")) {
                // GET /api/v1/songs -> Get all songs for the user
                // String orderByParam = request.getParameter("orderBy"); // OrderBy not implemented
                // in current SongDAO for this

                List<Song> userSongs = songDAO.findSongsByUser(user.getIdUser());
                List<SongWithAlbum> songsWithAlbumDetails = new ArrayList<>();

                for (Song song : userSongs) {
                    Album album = albumDAO.findAlbumById(song.getIdAlbum());
                    if (album != null) {
                        songsWithAlbumDetails.add(new SongWithAlbum(song, album));
                    } else {
                        logger.warn(
                                "Album with ID {} not found for song ID {} (User: {}). Skipping song in response.",
                                song.getIdAlbum(), song.getIdSong(), user.getUsername());
                    }
                }
                ResponseUtils.sendJson(response, HttpServletResponse.SC_OK, songsWithAlbumDetails);
                logger.debug("User {} retrieved {} songs.", user.getUsername(),
                        songsWithAlbumDetails.size());

            } else {
                // GET /api/v1/songs/{songId} -> Get a specific song
                String[] pathParts = pathInfo.substring(1).split("/");
                if (pathParts.length == 1) {
                    String songIdStr = pathParts[0];
                    int songIdInt; // Song IDs are int in the DAO
                    try {
                        songIdInt = Integer.parseInt(songIdStr);
                    } catch (NumberFormatException e) {
                        ResponseUtils.sendError(response, HttpServletResponse.SC_BAD_REQUEST,
                                "Invalid song ID format (must be an integer).");
                        return;
                    }

                    // SongDAO.findSongsByIdsAndUser expects a List and returns a List.
                    List<Integer> idsToFetch = new ArrayList<>();
                    idsToFetch.add(songIdInt);
                    List<Song> foundSongs =
                            songDAO.findSongsByIdsAndUser(idsToFetch, user.getIdUser());

                    if (foundSongs.isEmpty()) {
                        ResponseUtils.sendError(response, HttpServletResponse.SC_NOT_FOUND,
                                "Song not found or not owned by user.");
                    } else {
                        Song song = foundSongs.get(0);
                        Album album = albumDAO.findAlbumById(song.getIdAlbum());
                        if (album == null) {
                            logger.error(
                                    "Critical: Album ID {} for song ID {} (User: {}) not found in DB, but song exists.",
                                    song.getIdAlbum(), song.getIdSong(), user.getUsername());
                            ResponseUtils.sendError(response,
                                    HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                                    "Song data inconsistent: Album not found.");
                            return;
                        }
                        SongWithAlbum songWithAlbum = new SongWithAlbum(song, album);
                        ResponseUtils.sendJson(response, HttpServletResponse.SC_OK, songWithAlbum);
                        logger.debug("User {} retrieved song {}.", user.getUsername(), songIdInt);
                    }
                } else {
                    ResponseUtils.sendError(response, HttpServletResponse.SC_BAD_REQUEST,
                            "Invalid API path.");
                }
            }
        } catch (DAOException e) {
            logger.error("DAOException in doGet for user {}: Type={}, Message={}",
                    user.getUsername(), e.getErrorType(), e.getMessage(), e);
            ResponseUtils.sendError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Error processing request: " + e.getMessage());
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        HttpSession session = request.getSession(false);
        User user = (session != null) ? (User) session.getAttribute("user") : null;

        if (user == null) {
            ResponseUtils.sendError(response, HttpServletResponse.SC_UNAUTHORIZED,
                    "User not authenticated.");
            return;
        }

        String pathInfo = request.getPathInfo();
        if (pathInfo != null && !pathInfo.equals("/")) {
            ResponseUtils.sendError(response, HttpServletResponse.SC_METHOD_NOT_ALLOWED,
                    "POST method not allowed for this path.");
            return;
        }

        String songTitle = request.getParameter("title");
        String albumTitleParam = request.getParameter("albumTitle");
        String albumArtistParam = request.getParameter("albumArtist");
        String albumYearStr = request.getParameter("albumYear");
        String genreStr = request.getParameter("genre");

        if (songTitle == null || songTitle.isBlank() || albumTitleParam == null
                || albumTitleParam.isBlank() || albumArtistParam == null
                || albumArtistParam.isBlank() || albumYearStr == null || albumYearStr.isBlank()
                || genreStr == null || genreStr.isBlank()) {
            ResponseUtils.sendError(response, HttpServletResponse.SC_BAD_REQUEST,
                    "Missing required song metadata fields.");
            return;
        }

        int albumYear;
        try {
            albumYear = Integer.parseInt(albumYearStr);
            if (albumYear < 1000 || albumYear > 9999)
                throw new NumberFormatException("Album year out of range.");
        } catch (NumberFormatException e) {
            ResponseUtils.sendError(response, HttpServletResponse.SC_BAD_REQUEST,
                    "Invalid album year format: " + e.getMessage());
            return;
        }

        Genre genre;
        try {
            genre = Genre.valueOf(genreStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            ResponseUtils.sendError(response, HttpServletResponse.SC_BAD_REQUEST, "Invalid genre.");
            return;
        }

        Part audioFilePart = request.getPart("audioFile");
        if (audioFilePart == null || audioFilePart.getSize() == 0) {
            ResponseUtils.sendError(response, HttpServletResponse.SC_BAD_REQUEST,
                    "Audio file is required.");
            return;
        }

        int albumId;
        String imageFileStorageName = null;

        try {
            connection.setAutoCommit(false);

            Part imageFilePart = request.getPart("albumImage");
            if (imageFilePart != null && imageFilePart.getSize() > 0) {
                String imageFileName =
                        Paths.get(imageFilePart.getSubmittedFileName()).getFileName().toString();
                try (InputStream imageContent = imageFilePart.getInputStream()) {
                    // ImageDAO.saveImage does not take connection or userId
                    imageFileStorageName = imageDAO.saveImage(imageContent, imageFileName);
                }
            }

            // Attempt to find an existing album by title, artist, and year for the current user.
            // This logic might need a dedicated AlbumDAO.findAlbumByExactDetailsAndUser(title,
            // year, artist, userId)
            // For now, we'll try to find any album with these details and if not, create one for
            // this user.
            // identified.
            // Attempt to find an album by its name for the current user.
            // This simulates a AlbumDAO.findAlbumByNameAndUser(String name, UUID userId)
            List<Album> userAlbums = albumDAO.findAlbumsByUser(user.getIdUser());
            Album album =
                    userAlbums.stream().filter((x) -> x.getName().equalsIgnoreCase(albumTitleParam))
                            .findFirst().orElse(null);

            if (album == null) {
                logger.info(
                        "No album named '{}' found for user {}. Creating new album with artist '{}', year {}.",
                        albumTitleParam, user.getUsername(), albumArtistParam, albumYear);
                album = albumDAO.createAlbum(albumTitleParam, albumYear, albumArtistParam,
                        imageFileStorageName, user.getIdUser()); // imageFileStorageId can be null
            } else {
                logger.info("Found existing album '{}' (ID: {}) for user {}.", album.getName(),
                        album.getIdAlbum(), user.getUsername());
                // TODO: remove image
            }
            albumId = album.getIdAlbum();

            String audioFileName =
                    Paths.get(audioFilePart.getSubmittedFileName()).getFileName().toString();
            String audioFileStorageId;
            try (InputStream audioContent = audioFilePart.getInputStream()) {
                // AudioDAO.saveAudio does not take connection or userId
                audioFileStorageId = audioDAO.saveAudio(audioContent, audioFileName);
            }

            // SongDAO.createSong takes (title, idAlbum, year, genre, audioFile, idUser)
            // The 'year' here is the song's year, which is the album's year.
            Song createdSongBean = songDAO.createSong(songTitle, albumId, albumYear, genre,
                    audioFileStorageId, user.getIdUser());

            connection.commit();

            // Fetch the full SongWithAlbum details for the response
            Album finalAlbum = albumDAO.findAlbumById(albumId); // Re-fetch album to ensure
                                                                // consistency
            if (finalAlbum == null) { // Should not happen if create/find succeeded
                logger.error(
                        "Critical: Album ID {} for created song ID {} (User: {}) not found post-commit.",
                        albumId, createdSongBean.getIdSong(), user.getUsername());
                ResponseUtils.sendError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                        "Failed to retrieve album details for created song.");
                return;
            }
            SongWithAlbum responseSong = new SongWithAlbum(createdSongBean, finalAlbum);

            ResponseUtils.sendJson(response, HttpServletResponse.SC_CREATED, responseSong);
            logger.info("User {} created song {} (ID: {}) successfully.", user.getUsername(),
                    responseSong.getSong().getTitle(), responseSong.getSong().getIdSong());

        } catch (DAOException e) {
            try {
                if (connection != null)
                    connection.rollback();
            } catch (SQLException ex) {
                logger.error("Rollback failed after DAOException", ex);
            }
            // Log expected errors (validation, duplicates) with WARN, others with ERROR
            if (e.getErrorType() == DAOException.DAOErrorType.NAME_ALREADY_EXISTS || // For album
                                                                                     // name
                    e.getErrorType() == DAOException.DAOErrorType.DUPLICATE_ENTRY) { // For song in
                                                                                     // playlist
                                                                                     // (not
                                                                                     // relevant
                                                                                     // here but
                                                                                     // good
                                                                                     // practice)
                logger.warn(
                        "DAOException (expected type) during song creation for user {}: Type={}, Message={}",
                        user.getUsername(), e.getErrorType(), e.getMessage());
            } else {
                logger.error(
                        "DAOException (unexpected type) during song creation for user {}: Type={}, Message={}",
                        user.getUsername(), e.getErrorType(), e.getMessage(), e);
            }
            ResponseUtils.sendError(response,
                    (e.getErrorType() == DAOException.DAOErrorType.NAME_ALREADY_EXISTS
                            || e.getErrorType() == DAOException.DAOErrorType.NOT_FOUND)
                                    ? HttpServletResponse.SC_BAD_REQUEST
                                    : HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Error creating song: " + e.getMessage());
        } catch (SQLException e) {
            try {
                if (connection != null)
                    connection.rollback();
            } catch (SQLException ex) {
                logger.error("Rollback failed after SQLException", ex);
            }
            logger.error("SQLException during song creation for user {}: {}", user.getUsername(),
                    e.getMessage(), e);
            ResponseUtils.sendError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Database error during song creation: " + e.getMessage());
        } catch (IOException | ServletException e) {
            try {
                if (connection != null)
                    connection.rollback();
            } catch (SQLException ex) {
                logger.error("Rollback failed after File/Servlet Exception", ex);
            }
            logger.error("File processing or servlet error during song creation for user {}: {}",
                    user.getUsername(), e.getMessage(), e);
            ResponseUtils.sendError(response, HttpServletResponse.SC_BAD_REQUEST,
                    "Error processing uploaded file: " + e.getMessage());
        } finally {
            try {
                if (connection != null)
                    connection.setAutoCommit(true);
            } catch (SQLException ex) {
                logger.error("Failed to reset autoCommit to true", ex);
            }
        }
    }

    @Override
    public void destroy() {
        try {
            if (connection != null && !connection.isClosed()) {
                ConnectionHandler.closeConnection(connection);
                logger.info("Database connection closed for SongApiServlet.");
            }
        } catch (SQLException e) {
            logger.error("Failed to close database connection in destroy()", e);
        }
        super.destroy();
        logger.info("SongApiServlet destroyed.");
    }
}
