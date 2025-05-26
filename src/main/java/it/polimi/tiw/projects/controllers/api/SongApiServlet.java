package it.polimi.tiw.projects.controllers.api;

import it.polimi.tiw.projects.beans.*;
import it.polimi.tiw.projects.dao.*;
import it.polimi.tiw.projects.exceptions.DAOException;
import it.polimi.tiw.projects.listeners.AppContextListener;
import it.polimi.tiw.projects.utils.ConnectionHandler;
import it.polimi.tiw.projects.utils.Genre;
import it.polimi.tiw.projects.utils.ResponseUtils;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.UnavailableException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serial;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

@WebServlet("/api/v1/songs/*")
@MultipartConfig(fileSizeThreshold = 1024 * 1024, // 1MB
        maxFileSize = 1024 * 1024 * 75, // 75MB
        maxRequestSize = 1024 * 1024 * 85 // 85MB
)
public class SongApiServlet extends HttpServlet {
    @Serial
    private static final long serialVersionUID = 1L;
    private static final Logger logger = LoggerFactory.getLogger(SongApiServlet.class);

    private transient Connection connection;
    private transient SongDAO songDAO;
    private transient AlbumDAO albumDAO;
    private transient ImageDAO imageDAO;
    private transient AudioDAO audioDAO;
    private transient SongCreationServiceDAO songCreationServiceDAO;

    // Validation parameters
    private transient Pattern standardTextPattern;
    private transient Integer standardTextMinLength;
    private transient Integer standardTextMaxLength;

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
        if (pathInfo == null || pathInfo.equals("/") || pathInfo.isEmpty()) {
            return SongRoute.GET_ALL_SONGS;
        }

        Pattern audioPattern = Pattern.compile("^/(\\d+)/audio/?$");
        Matcher audioMatcher = audioPattern.matcher(pathInfo);
        if (audioMatcher.matches()) {
            return SongRoute.GET_SONG_AUDIO;
        }

        // Regex to match /<songId>/image or /<songId>/image/
        Pattern imagePattern = Pattern.compile("^/(\\d+)/image/?$");
        Matcher imageMatcher = imagePattern.matcher(pathInfo);
        if (imageMatcher.matches()) {
            return SongRoute.GET_SONG_IMAGE;
        }

        // Regex to match /genres or /genres/
        Pattern genresPattern = Pattern.compile("^/genres/?$");
        Matcher genresMatcher = genresPattern.matcher(pathInfo);
        if (genresMatcher.matches()) {
            return SongRoute.GET_SONG_GENRES;
        }

        // Regex to match /<songId> or /<songId>/
        Pattern byIdPattern = Pattern.compile("^/(\\d+)/?$");
        Matcher byIdMatcher = byIdPattern.matcher(pathInfo);
        if (byIdMatcher.matches()) {
            return SongRoute.GET_SONG_BY_ID;
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

        // Initialize the new SongCreationServiceDAO
        this.songCreationServiceDAO = new SongCreationServiceDAO(connection, this.albumDAO, this.songDAO, this.imageDAO,
                this.audioDAO);

        // Load consolidated validation parameters
        this.standardTextPattern = (Pattern) servletContext
                .getAttribute(AppContextListener.STANDARD_TEXT_REGEX_PATTERN);
        this.standardTextMinLength = (Integer) servletContext.getAttribute(AppContextListener.STANDARD_TEXT_MIN_LENGTH);
        this.standardTextMaxLength = (Integer) servletContext.getAttribute(AppContextListener.STANDARD_TEXT_MAX_LENGTH);

        if (standardTextPattern == null) {
            logger.warn(
                    "Standard text regex pattern (key: {}) not loaded from ServletContext. Validation for song/album titles and artists will be affected.",
                    AppContextListener.STANDARD_TEXT_REGEX_PATTERN);
        }
        if (standardTextMinLength == null) {
            logger.warn(
                    "Standard text min length (key: {}) not loaded from ServletContext. Min length validation for song/album titles and artists will be affected.",
                    AppContextListener.STANDARD_TEXT_MIN_LENGTH);
        }
        if (standardTextMaxLength == null) {
            logger.warn(
                    "Standard text max length (key: {}) not loaded from ServletContext. Max length validation for song/album titles and artists will be affected.",
                    AppContextListener.STANDARD_TEXT_MAX_LENGTH);
        }

        logger.info("SongApiServlet initialized successfully with SongCreationServiceDAO.");
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        User user = (session != null) ? (User) session.getAttribute("user") : null;

        if (user == null) {
            ResponseUtils.sendError(response, HttpServletResponse.SC_UNAUTHORIZED, "User not authenticated.");
            return;
        }

        SongRoute route = resolveRoute(request);
        String pathInfo = request.getPathInfo();
        logger.info("User {} - GET request. Path: '{}', Route: {}", user.getUsername(), pathInfo, route);

        String songIdStr = null;
        if (pathInfo != null && !pathInfo.equals("/") && !pathInfo.isEmpty()) {
            Pattern idExtractorPattern = Pattern.compile("^/(\\d+)(?:/.*)?$");
            Matcher idMatcher = idExtractorPattern.matcher(pathInfo);
            if (idMatcher.matches()) {
                songIdStr = idMatcher.group(1);
            }
        }

        try {
            switch (route) {
                case GET_ALL_SONGS:
                    handleGetAllSongs(response, user);
                    break;
                case GET_SONG_BY_ID:
                    handleGetSongById(response, user, songIdStr);
                    break;
                case GET_SONG_AUDIO:
                    handleGetSongAudio(response, user, songIdStr);
                    break;
                case GET_SONG_IMAGE:
                    handleGetSongImage(response, user, songIdStr);
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
            Song song = foundSongs.getFirst();
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
            song = foundSongs.getFirst();
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
            ResponseUtils.sendError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Error streaming audio file content.");
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
            Song song = foundSongs.getFirst();
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

    private Optional<String> validateStandardTextField(@NotBlank String rawValue, @NotBlank String fieldName,
                                                       HttpServletResponse response, @NotNull Pattern pattern, int minLength, int maxLength) {
        String trimmedValue = rawValue.trim();
        if (trimmedValue.isEmpty() || trimmedValue.length() < minLength || trimmedValue.length() > maxLength) {
            ResponseUtils.sendError(response, HttpServletResponse.SC_BAD_REQUEST,
                    fieldName + " must be between " + minLength + " and " + maxLength + " characters.");
            return Optional.empty();
        }
        if (!pattern.matcher(trimmedValue).matches()) {
            ResponseUtils.sendError(response, HttpServletResponse.SC_BAD_REQUEST,
                    fieldName + " contains invalid characters.");
            return Optional.empty();
        }
        return Optional.of(trimmedValue);
    }

    private Optional<SongCreationParameters> parseAndValidateSongCreationParameters(HttpServletRequest request,
                                                                                    HttpServletResponse response, User user) {

        String songTitleRaw = request.getParameter("title");
        String albumTitleRaw = request.getParameter("albumTitle");
        String albumArtistRaw = request.getParameter("albumArtist");
        String albumYearStr = request.getParameter("albumYear");
        String genreStr = request.getParameter("genre");

        // Presence Validation for all required text fields
        if (Stream.of(songTitleRaw, albumTitleRaw, albumArtistRaw, albumYearStr, genreStr)
                .anyMatch(s -> s == null || s.isBlank())) {
            ResponseUtils.sendError(response, HttpServletResponse.SC_BAD_REQUEST,
                    "Missing required song metadata fields (title, albumTitle, albumArtist, albumYear, genre).");
            return Optional.empty();
        }

        // Check if context validation parameters are loaded
        if (standardTextMinLength == null || standardTextMaxLength == null || standardTextPattern == null) {
            logger.error(
                    "CRITICAL: One or more standard text validation rules not loaded from context. standardTextMinLength: {}, standardTextMaxLength: {}, standardTextPattern: {}. Aborting song creation.",
                    standardTextMinLength, standardTextMaxLength, standardTextPattern);
            ResponseUtils.sendServiceUnavailableError(response,
                    "Server configuration error preventing song validation.");
            return Optional.empty();
        }

        Optional<String> songTitleOpt = validateStandardTextField(songTitleRaw, "Song title", response,
                standardTextPattern, standardTextMinLength, standardTextMaxLength);
        Optional<String> albumTitleOpt = validateStandardTextField(albumTitleRaw, "Album title", response,
                standardTextPattern, standardTextMinLength, standardTextMaxLength);
        Optional<String> albumArtistOpt = validateStandardTextField(albumArtistRaw, "Album artist", response,
                standardTextPattern, standardTextMinLength, standardTextMaxLength);

        List<Optional<?>> validatedFields = List.of(songTitleOpt, albumTitleOpt, albumArtistOpt);
        if (validatedFields.stream().anyMatch(Optional::isEmpty)) {
            return Optional.empty();
        }

        // Validate Album Year
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

        // Validate Genre
        Genre genre;
        try {
            genre = Genre.valueOf(genreStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            ResponseUtils.sendError(response, HttpServletResponse.SC_BAD_REQUEST, "Invalid genre: " + genreStr);
            return Optional.empty();
        }

        // Validate Audio File Part
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

        return Optional.of(new SongCreationParameters(songTitleOpt.orElseThrow(), albumTitleOpt.orElseThrow(),
                albumArtistOpt.orElseThrow(), albumYear, genre, audioFilePart));
    }

    private void handleCreateSong(HttpServletRequest request, HttpServletResponse response, User user) {
        logger.info("User {} attempting to create a new song.", user.getUsername());
        Optional<SongCreationParameters> paramsOpt = parseAndValidateSongCreationParameters(request, response, user);
        if (paramsOpt.isEmpty()) {
            return;
        }
        SongCreationParameters params = paramsOpt.get();
        Part imageFilePart = null;
        try {
            imageFilePart = request.getPart("albumImage");
        } catch (IOException | ServletException e) {
            logger.warn("Could not get albumImage part, proceeding without it. Error: {}", e.getMessage());
        }

        try {
            SongWithAlbum createdSongWithAlbum = songCreationServiceDAO.createSongWorkflow(user, params, imageFilePart);

            ResponseUtils.sendJson(response, HttpServletResponse.SC_CREATED, createdSongWithAlbum);
            logger.info("User {} created song {} (ID: {}) successfully.", user.getUsername(),
                    createdSongWithAlbum.getSong().getTitle(), createdSongWithAlbum.getSong().getIdSong());

        } catch (DAOException e) {
            logger.error("DAOException during song creation workflow for user {}: Type={}, Message={}",
                    user.getUsername(), e.getErrorType(), e.getMessage(), e);

            int statusCode = HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
            switch (e.getErrorType()) {
                case DUPLICATE_ENTRY, NOT_FOUND, NAME_ALREADY_EXISTS, IMAGE_SAVE_FAILED, AUDIO_SAVE_FAILED:
                    statusCode = HttpServletResponse.SC_BAD_REQUEST;
                    break;
                case CONSTRAINT_VIOLATION:
                    statusCode = HttpServletResponse.SC_CONFLICT;
                    break;
                default:
                    break;
            }
            ResponseUtils.sendError(response, statusCode, "Error creating song: " + e.getMessage());
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

    private enum SongRoute {
        // For GET requests
        GET_ALL_SONGS, GET_SONG_BY_ID, GET_SONG_AUDIO, GET_SONG_IMAGE, GET_SONG_GENRES,

        // For POST requests
        CREATE_SONG,

        // Fallback for unhandled or malformed paths
        INVALID_ROUTE
    }
}
