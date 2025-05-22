package it.polimi.tiw.projects.controllers.api;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    private transient Connection connection;
    private transient SongDAO songDAO;
    private transient AlbumDAO albumDAO;
    private transient ImageDAO imageDAO;
    private transient AudioDAO audioDAO;

    private enum SongRoute {
        // For GET requests
        GET_ALL_SONGS, GET_SONG_BY_ID, GET_SONG_AUDIO, GET_SONG_IMAGE, GET_SONG_GENRES,

        // For POST requests
        CREATE_SONG,

        // Fallback for unhandled or malformed paths
        INVALID_ROUTE
    }

    private static class SongCreationParameters {
        final String songTitle;
        final String albumTitle;
        final String albumArtist;
        final int albumYear;
        final Genre genre;
        final Part audioFilePart;

        public SongCreationParameters(String songTitle, String albumTitle, String albumArtist, int albumYear,
                Genre genre, Part audioFilePart) {
            this.songTitle = songTitle;
            this.albumTitle = albumTitle;
            this.albumArtist = albumArtist;
            this.albumYear = albumYear;
            this.genre = genre;
            this.audioFilePart = audioFilePart;
        }
    }

    private SongRoute resolveRoute(HttpServletRequest request) {
        String method = request.getMethod();
        String pathInfo = request.getPathInfo();

        if ("GET".equalsIgnoreCase(method)) {
            return resolveGetRoute(pathInfo);
        } else if ("POST".equalsIgnoreCase(method)) {
            return resolvePostRoute(pathInfo);
        }
        return SongRoute.INVALID_ROUTE;
    }

    private SongRoute resolveGetRoute(String pathInfo) {
        if (pathInfo == null || pathInfo.equals("/")) {
            return SongRoute.GET_ALL_SONGS;
        }

        String[] pathParts = pathInfo.substring(1).split("/");

        if (pathParts.length == 1) {
            if ("genres".equalsIgnoreCase(pathParts[0]))
                return SongRoute.GET_SONG_GENRES;

            return SongRoute.GET_SONG_BY_ID;
        }

        if (pathParts.length == 2) {
            String action = pathParts[1];
            if ("audio".equalsIgnoreCase(action)) {
                return SongRoute.GET_SONG_AUDIO;
            } else if ("image".equalsIgnoreCase(action)) {
                return SongRoute.GET_SONG_IMAGE;
            }
        }

        return SongRoute.INVALID_ROUTE;
    }

    private SongRoute resolvePostRoute(String pathInfo) {
        if (pathInfo == null || pathInfo.equals("/")) {
            return SongRoute.CREATE_SONG;
        }
        return SongRoute.INVALID_ROUTE;
    }

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        ServletContext servletContext = config.getServletContext();
        try {
            this.connection = ConnectionHandler.getConnection(servletContext);
        } catch (UnavailableException e) {
            throw new ServletException("Database connection unavailable", e);
        }

        this.songDAO = new SongDAO(connection);
        this.albumDAO = new AlbumDAO(connection);

        this.imageDAO = (ImageDAO) servletContext.getAttribute("imageDAO");
        this.audioDAO = (AudioDAO) servletContext.getAttribute("audioDAO");

        if (this.imageDAO == null || this.audioDAO == null) {
            logger.error("ImageDAO or AudioDAO not found in ServletContext. Check AppContextListener.");
            throw new ServletException("Critical file DAOs not initialized. Check AppContextListener setup.");
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
            ResponseUtils.sendError(response, HttpServletResponse.SC_UNAUTHORIZED, "User not authenticated.");
            return;
        }

        SongRoute route = resolveRoute(request);
        String pathInfo = request.getPathInfo();
        String[] pathParts = (pathInfo != null && !pathInfo.equals("/") && !pathInfo.isEmpty())
                ? pathInfo.substring(1).split("/")
                : new String[0];

        logger.info("User {} - GET request. Path: '{}', Route: {}", user.getUsername(), pathInfo, route);

        try {
            switch (route) {
            case GET_ALL_SONGS:
                handleGetAllSongs(response, user);
                break;
            case GET_SONG_BY_ID:
                handleGetSongById(response, user, pathParts[0]);
                break;
            case GET_SONG_AUDIO:
                handleGetSongAudio(response, user, pathParts[0]);
                break;
            case GET_SONG_IMAGE:
                handleGetSongImage(response, user, pathParts[0]);
                break;
            case GET_SONG_GENRES:
                handleGetSongGenres(response, user);
                break;
            default:
                ResponseUtils.sendError(response, HttpServletResponse.SC_BAD_REQUEST,
                        "Invalid API path or method not supported for this GET path.");
                break;
            }
        } catch (DAOException e) {
            logger.error("DAOException in doGet for user {}: Type={}, Message={}", user.getUsername(), e.getErrorType(),
                    e.getMessage(), e);
            ResponseUtils.sendError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Error processing request: " + e.getMessage());
        }
    }

    private void handleGetAllSongs(HttpServletResponse response, User user) throws DAOException {
        logger.info("User {} attempting to get all songs.", user.getUsername());
        List<Song> userSongs = songDAO.findSongsByUser(user.getIdUser());
        List<SongWithAlbum> songsWithAlbumDetails = new ArrayList<>();

        for (Song song : userSongs) {
            Album album = albumDAO.findAlbumById(song.getIdAlbum());
            if (album != null) {
                songsWithAlbumDetails.add(new SongWithAlbum(song, album));
            } else {
                logger.warn("Album with ID {} not found for song ID {} (User: {}). Skipping song in response.",
                        song.getIdAlbum(), song.getIdSong(), user.getUsername());
            }
        }
        ResponseUtils.sendJson(response, HttpServletResponse.SC_OK, songsWithAlbumDetails);
        logger.debug("User {} retrieved {} songs.", user.getUsername(), songsWithAlbumDetails.size());
    }

    private void handleGetSongById(HttpServletResponse response, User user, String songIdStr) throws DAOException {
        logger.info("User {} attempting to get song by ID: {}", user.getUsername(), songIdStr);
        int songIdInt;
        try {
            songIdInt = Integer.parseInt(songIdStr);
        } catch (NumberFormatException e) {
            ResponseUtils.sendError(response, HttpServletResponse.SC_BAD_REQUEST,
                    "Invalid song ID format (must be an integer).");
            return;
        }

        List<Integer> idsToFetch = new ArrayList<>();
        idsToFetch.add(songIdInt);
        List<Song> foundSongs;
        foundSongs = songDAO.findSongsByIdsAndUser(idsToFetch, user.getIdUser());

        if (foundSongs.isEmpty()) {
            ResponseUtils.sendError(response, HttpServletResponse.SC_NOT_FOUND, "Song not found or not owned by user.");
        } else {
            Song song = foundSongs.get(0);
            Album album = null;
            try {
                album = albumDAO.findAlbumById(song.getIdAlbum());
            } catch (DAOException e) {
                if (e.getErrorType() == DAOException.DAOErrorType.NOT_FOUND) {
                    logger.error("Critical: Album ID {} for song ID {} (User: {}) not found in DB, but song exists.",
                            song.getIdAlbum(), song.getIdSong(), user.getUsername());
                    ResponseUtils.sendError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                            "Song data inconsistent: Album not found.");
                    return;
                }
            }
            SongWithAlbum songWithAlbum = new SongWithAlbum(song, album);
            ResponseUtils.sendJson(response, HttpServletResponse.SC_OK, songWithAlbum);
            logger.debug("User {} retrieved song {}.", user.getUsername(), songIdInt);
        }
    }

    private void handleGetSongAudio(HttpServletResponse response, User user, String songIdStr) {
        int songIdInt;
        try {
            songIdInt = Integer.parseInt(songIdStr);
        } catch (NumberFormatException e) {
            ResponseUtils.sendError(response, HttpServletResponse.SC_BAD_REQUEST, "Invalid song ID format.");
            return;
        }

        Song song;
        String audioStorageName = null;

        try {
            List<Song> foundSongs = songDAO.findSongsByIdsAndUser(List.of(songIdInt), user.getIdUser());
            if (foundSongs.isEmpty()) {
                ResponseUtils.sendError(response, HttpServletResponse.SC_NOT_FOUND,
                        "Song not found or not accessible by user.");
                return;
            }
            song = foundSongs.get(0);
            audioStorageName = song.getAudioFile();

        } catch (DAOException e) {
            logger.error(
                    "DAOException while fetching song metadata for audio streaming (Song ID {}, User {}): Type={}, Message={}",
                    songIdInt, user.getUsername(), e.getErrorType(), e.getMessage(), e);
            if (e.getErrorType() == DAOException.DAOErrorType.NOT_FOUND) {
                ResponseUtils.sendError(response, HttpServletResponse.SC_NOT_FOUND,
                        "Song not found or not accessible.");
            } else {
                ResponseUtils.sendError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                        "Error retrieving song details.");
            }
        }

        if (audioStorageName == null || audioStorageName.isBlank()) {
            logger.error("Song ID {} for user {} has no associated audio file name.", songIdInt, user.getUsername());
            ResponseUtils.sendError(response, HttpServletResponse.SC_NOT_FOUND,
                    "Audio file not available for this song.");
            return;
        }

        try (FileData audioFileData = audioDAO.getAudio(audioStorageName)) {

            response.setContentType(audioFileData.getMimeType());
            response.setContentLengthLong(audioFileData.getSize());

            response.setHeader("Content-Disposition", "inline; filename=\"" + audioStorageName + "\"");

            try (InputStream audioStream = audioFileData.getContent();
                    BufferedOutputStream bufferedResponseStream = new BufferedOutputStream(
                            response.getOutputStream())) {
                audioStream.transferTo(bufferedResponseStream);
                bufferedResponseStream.flush();
                logger.debug("Successfully streamed audio for song ID {} for user {}", songIdInt, user.getUsername());
            }

        } catch (DAOException e) {
            logger.error(
                    "DAOException from AudioDAO.getAudio for storage name {} (Song ID {}, User {}): Type={}, Message={}",
                    audioStorageName, songIdInt, user.getUsername(), e.getErrorType(), e.getMessage(), e);
            if (e.getErrorType() == DAOException.DAOErrorType.NOT_FOUND) {
                ResponseUtils.sendError(response, HttpServletResponse.SC_NOT_FOUND, "Audio file content not found.");
            } else {
                ResponseUtils.sendError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                        "Error accessing audio file content.");
            }
        } catch (IllegalArgumentException e) {
            logger.warn("IllegalArgumentException from AudioDAO.getAudio for storage name {} (Song ID {}, User {}): {}",
                    audioStorageName, songIdInt, user.getUsername(), e.getMessage());
            ResponseUtils.sendError(response, HttpServletResponse.SC_BAD_REQUEST,
                    "Invalid request for audio file: " + e.getMessage());
        } catch (IOException e) {
            logger.error(
                    "IOException from FileData.close() or input stream operations for song ID {} (user {}), storage name {}: {}",
                    songIdInt, user.getUsername(), audioStorageName, e.getMessage(), e);
        }
    }

    private void handleGetSongImage(HttpServletResponse response, User user, String songIdStr) {
        int songIdInt;
        try {
            songIdInt = Integer.parseInt(songIdStr);
        } catch (NumberFormatException e) {
            ResponseUtils.sendError(response, HttpServletResponse.SC_BAD_REQUEST, "Invalid song ID format.");
            return;
        }

        String imageStorageName = null;
        Album album = null;
        int albumId = -1;

        try {
            List<Song> foundSongs = songDAO.findSongsByIdsAndUser(List.of(songIdInt), user.getIdUser());
            if (foundSongs.isEmpty()) {
                ResponseUtils.sendError(response, HttpServletResponse.SC_NOT_FOUND,
                        "Song not found or not accessible by user.");
                return;
            }
            Song song = foundSongs.get(0);
            albumId = song.getIdAlbum();

            album = albumDAO.findAlbumById(albumId);

            imageStorageName = album.getImageFile();

        } catch (DAOException e) {
            logger.error(
                    "DAOException while fetching song/album metadata for image streaming (Song ID {}, User {}): Type={}, Message={}",
                    songIdInt, user.getUsername(), e.getErrorType(), e.getMessage(), e);
            ResponseUtils.sendError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Error retrieving song/album details.");
        }

        if (album == null) {
            logger.warn("Album ID {} for song ID {} not found.", albumId, songIdInt);
            ResponseUtils.sendError(response, HttpServletResponse.SC_NOT_FOUND,
                    "Album associated with the song not found.");
            return;
        }

        if (imageStorageName == null || imageStorageName.isBlank()) {
            logger.warn("Album ID {} (from song ID {}) has no associated image file name.", albumId, songIdInt);
            ResponseUtils.sendError(response, HttpServletResponse.SC_NOT_FOUND,
                    "Image not available for this song's album.");
            return;
        }

        try (FileData imageFileData = imageDAO.getImage(imageStorageName)) {
            response.setContentType(imageFileData.getMimeType());
            response.setContentLengthLong(imageFileData.getSize());

            String albumNameForFile = album.getName().replaceAll("[^a-zA-Z0-9.-]", "_");
            response.setHeader("Content-Disposition", "inline; filename=\"" + albumNameForFile + "\"");

            try (InputStream imageStream = imageFileData.getContent();
                    BufferedOutputStream bufferedResponseStream = new BufferedOutputStream(
                            response.getOutputStream())) {
                imageStream.transferTo(bufferedResponseStream);
                bufferedResponseStream.flush();
                logger.debug("Successfully streamed image for album ID {} (from song ID {}) for user {}", albumId,
                        songIdInt, user.getUsername());
            }
        } catch (DAOException e) {
            logger.error(
                    "DAOException from ImageDAO.getImage for storage name {} (Album ID {}, Song ID {}): Type={}, Message={}",
                    imageStorageName, albumId, songIdInt, e.getErrorType(), e.getMessage(), e);
            if (e.getErrorType() == DAOException.DAOErrorType.NOT_FOUND) {
                ResponseUtils.sendError(response, HttpServletResponse.SC_NOT_FOUND, "Image file content not found.");
            } else {
                ResponseUtils.sendError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                        "Error accessing image file content.");
            }
        } catch (IllegalArgumentException e) {
            logger.warn(
                    "IllegalArgumentException from ImageDAO.getImage for storage name {} (Album ID {}, Song ID {}): {}",
                    imageStorageName, albumId, songIdInt, e.getMessage());
            ResponseUtils.sendError(response, HttpServletResponse.SC_BAD_REQUEST,
                    "Invalid request for image file: " + e.getMessage());
        } catch (IOException e) {
            logger.error("IOException from FileData.close() for album ID {} (song ID {}), storage name {}: {}", albumId,
                    songIdInt, imageStorageName, e.getMessage(), e);
        }
    }

    private void handleGetSongGenres(HttpServletResponse response, User user) {
        logger.info("User {} requesting list of all song genres.", user.getUsername());
        List<Map<String, String>> genreList = Stream.of(Genre.values()).map(genre -> {
            Map<String, String> genreMap = new HashMap<>();
            genreMap.put("name", genre.name());
            genreMap.put("description", genre.getDescription());
            return genreMap;
        }).toList();

        ResponseUtils.sendJson(response, HttpServletResponse.SC_OK, genreList);
        logger.info("Successfully sent list of {} genres to user {}.", genreList.size(), user.getUsername());
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        HttpSession session = request.getSession(false);
        User user = (session != null) ? (User) session.getAttribute("user") : null;

        if (user == null) {
            ResponseUtils.sendError(response, HttpServletResponse.SC_UNAUTHORIZED, "User not authenticated.");
            return;
        }

        SongRoute route = resolveRoute(request);
        String pathInfo = request.getPathInfo();
        logger.info("User {} - POST request. Path: '{}', Route: {}", user.getUsername(), pathInfo, route);

        if (route.compareTo(SongRoute.CREATE_SONG) == 0)
            handleCreateSong(request, response, user);
        else
            ResponseUtils.sendError(response, HttpServletResponse.SC_METHOD_NOT_ALLOWED,
                    "POST method not allowed for this specific path or path is invalid.");
    }

    private Optional<SongCreationParameters> parseAndValidateSongCreationParameters(HttpServletRequest request,
            HttpServletResponse response, User user) {
        String songTitle = request.getParameter("title");
        String albumTitleParam = request.getParameter("albumTitle");
        String albumArtistParam = request.getParameter("albumArtist");
        String albumYearStr = request.getParameter("albumYear");
        String genreStr = request.getParameter("genre");

        if (Stream.of(songTitle, albumTitleParam, albumArtistParam, albumYearStr, genreStr)
                .anyMatch(s -> s == null || s.isBlank())) {
            ResponseUtils.sendError(response, HttpServletResponse.SC_BAD_REQUEST,
                    "Missing required song metadata fields.");
            return Optional.empty();
        }

        int albumYear;
        try {
            albumYear = Integer.parseInt(albumYearStr);
            if (albumYear < 1000 || albumYear > 9999) {
                throw new NumberFormatException("Album year out of range.");
            }
        } catch (NumberFormatException e) {
            ResponseUtils.sendError(response, HttpServletResponse.SC_BAD_REQUEST,
                    "Invalid album year format: " + e.getMessage());
            return Optional.empty();
        }

        Genre genre;
        try {
            genre = Genre.valueOf(genreStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            ResponseUtils.sendError(response, HttpServletResponse.SC_BAD_REQUEST, "Invalid genre.");
            return Optional.empty();
        }

        Part audioFilePart;
        try {
            audioFilePart = request.getPart("audioFile");
            if (audioFilePart == null || audioFilePart.getSize() == 0) {
                ResponseUtils.sendError(response, HttpServletResponse.SC_BAD_REQUEST, "Audio file is required.");
                return Optional.empty();
            }
        } catch (IOException | ServletException e) {
            logger.error("Error processing uploaded audio file part for user {}: {}", user.getUsername(),
                    e.getMessage(), e);
            ResponseUtils.sendError(response, HttpServletResponse.SC_BAD_REQUEST,
                    "Error processing uploaded audio file: " + e.getMessage());
            return Optional.empty();
        }

        return Optional.of(new SongCreationParameters(songTitle, albumTitleParam, albumArtistParam, albumYear, genre,
                audioFilePart));
    }

    private void handleCreateSong(HttpServletRequest request, HttpServletResponse response, User user) {
        logger.info("User {} attempting to create a new song.", user.getUsername());
        Optional<SongCreationParameters> paramsOpt = parseAndValidateSongCreationParameters(request, response, user);
        if (paramsOpt.isEmpty()) {
            return;
        }
        SongCreationParameters params = paramsOpt.get();

        int albumId;
        String audioFileStorageName = null;
        // TODO: ensure better song creations process
        try {
            // Transaction starts
            connection.setAutoCommit(false);

            // Save audio first to ensure it's a correct file
            audioFileStorageName = saveAudioToDisk(params.audioFilePart);

            List<Album> userAlbums = albumDAO.findAlbumsByUser(user.getIdUser());
            Album album = userAlbums.stream().filter(x -> x.getName().equalsIgnoreCase(params.albumTitle)).findFirst()
                    .orElse(null);

            if (album == null) {
                Part imageFilePart = request.getPart("albumImage");
                String imageFileStorageName = null;
                if (imageFilePart != null && imageFilePart.getSize() > 0) {
                    imageFileStorageName = saveImageToDisk(imageFilePart);
                }
                logger.info("No album named '{}' found for user {}. Creating new album with artist '{}', year {}.",
                        params.albumTitle, user.getUsername(), params.albumArtist, params.albumYear);

                album = albumDAO.createAlbum(params.albumTitle, params.albumYear, params.albumArtist,
                        imageFileStorageName, user.getIdUser());
            } else {
                logger.info("Found existing album '{}' (ID: {}) for user {}.", album.getName(), album.getIdAlbum(),
                        user.getUsername());
            }

            albumId = album.getIdAlbum();
            Song createdSongBean = songDAO.createSong(params.songTitle, albumId, params.albumYear, params.genre,
                    audioFileStorageName, user.getIdUser());

            connection.commit();

            SongWithAlbum responseSong = new SongWithAlbum(createdSongBean, album);
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
            if (e.getErrorType() == DAOException.DAOErrorType.NAME_ALREADY_EXISTS
                    || e.getErrorType() == DAOException.DAOErrorType.DUPLICATE_ENTRY) {
                logger.warn("DAOException (expected type) during song creation for user {}: Type={}, Message={}",
                        user.getUsername(), e.getErrorType(), e.getMessage());
            } else {
                logger.error("DAOException (unexpected type) during song creation for user {}: Type={}, Message={}",
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
                audioDAO.deleteAudio(audioFileStorageName);
            } catch (SQLException | DAOException ex) {
                logger.error("Rollback failed and audio deletion after Exception", ex);
            }
            logger.error("SQLException during song creation for user {}: {}", user.getUsername(), e.getMessage(), e);
            ResponseUtils.sendError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Database error during song creation: " + e.getMessage());
        } catch (IOException | ServletException e) {
            try {
                if (connection != null)
                    connection.rollback();
                audioDAO.deleteAudio(audioFileStorageName);
            } catch (SQLException | DAOException ex) {
                logger.error("Rollback failed after File/Servlet Exception or DAOException during deletion audio", ex);
            }
            logger.error("File processing or servlet error during song creation for user {}: {}", user.getUsername(),
                    e.getMessage(), e);
            ResponseUtils.sendError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Error processing uploaded file or request data: " + e.getMessage());
        } finally {
            try {
                if (connection != null)
                    connection.setAutoCommit(true);
            } catch (SQLException ex) {
                logger.error("Failed to reset autoCommit to true", ex);
            }
        }
    }

    private String saveAudioToDisk(Part audioFilePart) throws DAOException, IOException {
        String audioFileName = Paths.get(audioFilePart.getSubmittedFileName()).getFileName().toString();
        String audioFileStorageName;
        try (InputStream audioContent = audioFilePart.getInputStream()) {
            audioFileStorageName = audioDAO.saveAudio(audioContent, audioFileName);
        }
        return audioFileStorageName;
    }

    private String saveImageToDisk(Part imageFilePart) throws DAOException, IOException {
        String imageFileName = Paths.get(imageFilePart.getSubmittedFileName()).getFileName().toString();
        String imageFileStorageName;
        try (InputStream imageContent = imageFilePart.getInputStream()) {
            imageFileStorageName = imageDAO.saveImage(imageContent, imageFileName);
        }
        return imageFileStorageName;
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
