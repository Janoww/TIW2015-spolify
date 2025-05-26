package it.polimi.tiw.projects.controllers.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import it.polimi.tiw.projects.beans.*;
import it.polimi.tiw.projects.dao.PlaylistDAO;
import it.polimi.tiw.projects.dao.PlaylistOrderDAO;
import it.polimi.tiw.projects.exceptions.DAOException;
import it.polimi.tiw.projects.listeners.AppContextListener;
import it.polimi.tiw.projects.utils.ConnectionHandler;
import it.polimi.tiw.projects.utils.ObjectMapperUtils;
import it.polimi.tiw.projects.utils.ResponseUtils;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.UnavailableException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Serial;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@WebServlet("/api/v1/playlists/*")
public class PlaylistApiServlet extends HttpServlet {
    @Serial
    private static final long serialVersionUID = 1L;
    private static final Logger logger = LoggerFactory.getLogger(PlaylistApiServlet.class);

    private transient Connection connection;
    private transient PlaylistDAO playlistDAO;
    private transient PlaylistOrderDAO playlistOrderDAO;

    // Validation parameters
    private transient Pattern playlistNamePattern;
    private transient Integer playlistNameMinLength;
    private transient Integer playlistNameMaxLength;

    private PlaylistActionRoute resolveRoute(HttpServletRequest request) {
        String method = request.getMethod();
        String pathInfo = request.getPathInfo();

        if ("GET".equalsIgnoreCase(method)) {
            if (pathInfo == null || pathInfo.equals("/") || pathInfo.isEmpty()) {
                return PlaylistActionRoute.GET_USER_PLAYLISTS;
            }
            Pattern orderPattern = Pattern.compile("^/(\\d+)/order/?$");
            Matcher orderMatcher = orderPattern.matcher(pathInfo);
            if (orderMatcher.matches()) {
                return PlaylistActionRoute.GET_PLAYLIST_ORDER;
            }
        } else if ("POST".equalsIgnoreCase(method)) {
            if (pathInfo == null || pathInfo.equals("/") || pathInfo.isEmpty()) {
                return PlaylistActionRoute.CREATE_PLAYLIST;
            }
            Pattern addSongsPattern = Pattern.compile("^/(\\d+)/songs/?$");
            Matcher addSongsMatcher = addSongsPattern.matcher(pathInfo);
            if (addSongsMatcher.matches()) {
                return PlaylistActionRoute.ADD_SONGS_TO_PLAYLIST;
            }
        } else if ("PUT".equalsIgnoreCase(method)) {
            Pattern orderPattern = Pattern.compile("^/(\\d+)/order/?$");
            Matcher orderMatcher = orderPattern.matcher(pathInfo);
            if (orderMatcher.matches()) {
                return PlaylistActionRoute.UPDATE_PLAYLIST_ORDER;
            }
        }
        return PlaylistActionRoute.INVALID_ROUTE;
    }

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

        this.playlistDAO = new PlaylistDAO(connection);
        this.playlistOrderDAO = (PlaylistOrderDAO) servletContext.getAttribute("playlistOrderDAO");
        if (this.playlistOrderDAO == null) {
            logger.error("PlaylistOrderDAO not found in ServletContext. Check AppContextListener.");
            throw new ServletException("Critical PlaylistOrderDAO not initialized.");
        }

        // Load validation patterns from ServletContext
        this.playlistNamePattern = (Pattern) servletContext
                .getAttribute(AppContextListener.PLAYLIST_NAME_REGEX_PATTERN);

        this.playlistNameMinLength = (Integer) servletContext.getAttribute(AppContextListener.STANDARD_TEXT_MIN_LENGTH);
        this.playlistNameMaxLength = (Integer) servletContext.getAttribute(AppContextListener.STANDARD_TEXT_MAX_LENGTH);

        if (this.playlistNamePattern == null) {
            logger.warn(
                    "Playlist name validation regex pattern (key: {}) not found in ServletContext. Regex validation for playlist name will be skipped.",
                    AppContextListener.PLAYLIST_NAME_REGEX_PATTERN);
        }
        if (this.playlistNameMinLength == null) {
            logger.warn(
                    "Standard text min length (key: {}) not found in ServletContext. Min length validation for playlist name will be skipped.",
                    AppContextListener.STANDARD_TEXT_MIN_LENGTH);
        }
        if (this.playlistNameMaxLength == null) {
            logger.warn(
                    "Standard text max length (key: {}) not found in ServletContext. Max length validation for playlist name will be skipped.",
                    AppContextListener.STANDARD_TEXT_MAX_LENGTH);
        }

        logger.info("PlaylistApiServlet initialized successfully.");
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) {
        HttpSession session = request.getSession(false);
        User user = (session != null) ? (User) session.getAttribute("user") : null;

        if (user == null) {
            logger.warn("Unauthorized GET attempt: No user in session. Path: {}", request.getPathInfo());
            ResponseUtils.sendError(response, HttpServletResponse.SC_UNAUTHORIZED, "User not authenticated.");
            return;
        }

        PlaylistActionRoute route = resolveRoute(request);
        String pathInfo = request.getPathInfo();
        logger.info("User {} - GET request. Path: '{}', Route: {}", user.getUsername(),
                (pathInfo != null ? pathInfo : "/"), route);

        String[] pathParts = (pathInfo != null && pathInfo.length() > 1) ? pathInfo.substring(1).split("/")
                : new String[0];

        try {
            switch (route) {
            case GET_USER_PLAYLISTS:
                handleGetUserPlaylists(response, user);
                break;
            case GET_PLAYLIST_ORDER:
                handleGetPlaylistOrder(response, user, pathParts[0]);
                break;
            default:
                logger.warn("Invalid route for GET request by user {}: Path='{}', Resolved='{}'", user.getUsername(),
                        pathInfo, route);
                ResponseUtils.sendError(response, HttpServletResponse.SC_NOT_FOUND, "Endpoint not found.");
                break;
            }
        } catch (DAOException e) {
            logger.error("DAOException in GET for user {}: Type={}, Message={}", user.getUsername(), e.getErrorType(),
                    e.getMessage(), e);
            ResponseUtils.sendError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Error processing request: " + e.getMessage());
        }
    }

    private void handleGetUserPlaylists(HttpServletResponse response, User user) throws DAOException {
        List<Playlist> playlists = playlistDAO.findPlaylistsByUser(user.getIdUser());
        ResponseUtils.sendJson(response, HttpServletResponse.SC_OK, playlists);
        logger.info("User {} retrieved {} playlists successfully.", user.getUsername(), playlists.size());
    }

    private void handleGetPlaylistOrder(HttpServletResponse response, User user, String playlistIdStr)
            throws DAOException {
        int playlistId;
        try {
            playlistId = Integer.parseInt(playlistIdStr);
        } catch (NumberFormatException e) {
            logger.warn("Invalid playlist ID format '{}' in GET request for user {}.", playlistIdStr,
                    user.getUsername());
            ResponseUtils.sendError(response, HttpServletResponse.SC_BAD_REQUEST, "Invalid playlist ID format.");
            return;
        }
        try {
            playlistDAO.findPlaylistById(playlistId, user.getIdUser());
        } catch (DAOException e) {
            if (e.getErrorType() == DAOException.DAOErrorType.NOT_FOUND
                    || e.getErrorType() == DAOException.DAOErrorType.ACCESS_DENIED) {
                logger.warn("User {} attempted to get order for playlist {} they don't own or doesn't exist.",
                        user.getUsername(), playlistId);
                ResponseUtils.sendError(response, HttpServletResponse.SC_NOT_FOUND,
                        "Playlist not found or access denied.");
                return;
            }
            throw e;
        }

        List<Integer> order = playlistOrderDAO.getPlaylistOrder(playlistId);
        ResponseUtils.sendJson(response, HttpServletResponse.SC_OK, order);
        logger.info("User {} retrieved order for playlist {}.", user.getUsername(), playlistId);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) {
        HttpSession session = request.getSession(false);
        User user = (session != null) ? (User) session.getAttribute("user") : null;

        if (user == null) {
            ResponseUtils.sendError(response, HttpServletResponse.SC_UNAUTHORIZED, "User not authenticated.");
            return;
        }

        String contentType = request.getContentType();
        if (contentType == null || !contentType.toLowerCase().startsWith("application/json")) {
            ResponseUtils.sendError(response, HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE,
                    "Content-Type must be application/json for this operation.");
            return;
        }

        PlaylistActionRoute route = resolveRoute(request);
        String pathInfo = request.getPathInfo();
        logger.info("User {} - POST request. Path: '{}', Route: {}", user.getUsername(),
                (pathInfo != null ? pathInfo : "/"), route);

        String[] pathParts = (pathInfo != null && pathInfo.length() > 1) ? pathInfo.substring(1).split("/")
                : new String[0];

        switch (route) {
        case CREATE_PLAYLIST:
            handleCreatePlaylist(request, response, user);
            break;
        case ADD_SONGS_TO_PLAYLIST:
            handleAddSongsToPlaylist(request, response, user, pathParts[0]);
            break;
        default:
            logger.warn("Invalid route for POST request by user {}: Path='{}', Resolved='{}'", user.getUsername(),
                    pathInfo, route);
            ResponseUtils.sendError(response, HttpServletResponse.SC_NOT_FOUND, "Endpoint not found.");
            break;
        }
    }

    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response) {
        HttpSession session = request.getSession(false);
        User user = (session != null) ? (User) session.getAttribute("user") : null;

        if (user == null) {
            ResponseUtils.sendError(response, HttpServletResponse.SC_UNAUTHORIZED, "User not authenticated.");
            return;
        }

        String contentType = request.getContentType();
        if (contentType == null || !contentType.toLowerCase().startsWith("application/json")) {
            ResponseUtils.sendError(response, HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE,
                    "Content-Type must be application/json for this operation.");
            return;
        }

        PlaylistActionRoute route = resolveRoute(request);
        String pathInfo = request.getPathInfo();
        logger.info("User {} - PUT request. Path: '{}', Route: {}", user.getUsername(),
                (pathInfo != null ? pathInfo : "/"), route);

        String[] pathParts = (pathInfo != null && pathInfo.length() > 1) ? pathInfo.substring(1).split("/")
                : new String[0];

        if (route != PlaylistActionRoute.UPDATE_PLAYLIST_ORDER) {
            logger.warn("Invalid route for PUT request by user {}: Path='{}', Resolved='{}'", user.getUsername(),
                    pathInfo, route);
            ResponseUtils.sendError(response, HttpServletResponse.SC_NOT_FOUND, "Endpoint not found.");
            return;
        }

        handleUpdatePlaylistOrder(request, response, user, pathParts[0]);
    }

    private Optional<Integer> parseAndValidatePlaylistId(String playlistIdStr, HttpServletResponse response,
            User user) {
        try {
            int playlistId = Integer.parseInt(playlistIdStr);
            if (playlistId <= 0) {
                logger.warn("Invalid playlist ID format (non-positive) '{}' in PUT request for user {}.", playlistIdStr,
                        user.getUsername());
                ResponseUtils.sendError(response, HttpServletResponse.SC_BAD_REQUEST,
                        "Playlist ID must be a positive integer.");
                return Optional.empty();
            }
            return Optional.of(playlistId);
        } catch (NumberFormatException e) {
            logger.warn("Invalid playlist ID format '{}' in PUT request for user {}.", playlistIdStr,
                    user.getUsername());
            ResponseUtils.sendError(response, HttpServletResponse.SC_BAD_REQUEST, "Invalid playlist ID format.");
            return Optional.empty();
        }
    }

    private Optional<Playlist> verifyPlaylistOwnershipAndExistence(int playlistId, User user,
            HttpServletResponse response) {
        try {
            Playlist dbPlaylist = playlistDAO.findPlaylistById(playlistId, user.getIdUser());
            return Optional.of(dbPlaylist);
        } catch (DAOException e) {
            if (e.getErrorType() == DAOException.DAOErrorType.NOT_FOUND
                    || e.getErrorType() == DAOException.DAOErrorType.ACCESS_DENIED) {
                logger.warn("User {} - Update order failed: Playlist {} not found or access denied.",
                        user.getUsername(), playlistId);
                ResponseUtils.sendError(response, HttpServletResponse.SC_NOT_FOUND,
                        "Playlist not found or access denied.");
            } else {
                logger.error("User {} - DAOException while fetching playlist {} for order update: {}",
                        user.getUsername(), playlistId, e.getMessage(), e);
                ResponseUtils.sendError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                        "Error fetching playlist details.");
            }
            return Optional.empty();
        }
    }

    private Optional<List<Integer>> parseSongIdsFromRequest(HttpServletRequest request, HttpServletResponse response,
            User user, int playlistId) {
        try {
            List<Integer> rawClientSongIds = ObjectMapperUtils.getMapper().readValue(request.getInputStream(),
                    new TypeReference<>() {
                    });
            if (rawClientSongIds == null) {
                logger.warn("User {} - Update order for playlist {}: Received null song ID list.", user.getUsername(),
                        playlistId);
                ResponseUtils.sendError(response, HttpServletResponse.SC_BAD_REQUEST, "Song ID list cannot be null.");
                return Optional.empty();
            }
            return Optional.of(rawClientSongIds);
        } catch (JsonProcessingException e) {
            logger.warn("User {} - Update order for playlist {}: Invalid JSON format. Error: {}", user.getUsername(),
                    playlistId, e.getMessage());
            ResponseUtils.sendError(response, HttpServletResponse.SC_BAD_REQUEST,
                    "Invalid JSON format: " + e.getOriginalMessage());
            return Optional.empty();
        } catch (IOException e) {
            logger.error("User {} - Update order for playlist {}: IOException reading request body. Error: {}",
                    user.getUsername(), playlistId, e.getMessage(), e);
            ResponseUtils.sendError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Error reading request data.");
            return Optional.empty();
        }
    }

    private Optional<List<Integer>> processAndValidateSongOrder(List<Integer> rawClientSongIds, Playlist dbPlaylist,
            HttpServletResponse response, User user, int playlistId) {
        Set<Integer> uniqueOrderedSongIdsSet = new LinkedHashSet<>(rawClientSongIds);

        if (rawClientSongIds.size() != uniqueOrderedSongIdsSet.size()) {
            logger.info(
                    "User {} - Update order for playlist {}: Duplicate song IDs were present in the input. Original count: {}, Unique count: {}. Proceeding with unique, ordered list.",
                    user.getUsername(), playlistId, rawClientSongIds.size(), uniqueOrderedSongIdsSet.size());
        }

        List<Integer> dbSongIds = dbPlaylist.getSongs();
        Set<Integer> dbSongIdsSet = new HashSet<>(dbSongIds);

        if (uniqueOrderedSongIdsSet.size() != dbSongIds.size()) {
            logger.warn(
                    "User {} - Update order for playlist {}: Processed client list size ({}) does not match DB list size ({}). Original client input size was {}.",
                    user.getUsername(), playlistId, uniqueOrderedSongIdsSet.size(), dbSongIds.size(),
                    rawClientSongIds.size());
            ResponseUtils.sendError(response, HttpServletResponse.SC_BAD_REQUEST,
                    "Submitted order count (after removing duplicates: " + uniqueOrderedSongIdsSet.size()
                            + ") does not match actual song count in playlist (" + dbSongIds.size() + ").");
            return Optional.empty();
        }

        if (!uniqueOrderedSongIdsSet.equals(dbSongIdsSet)) {
            logger.warn(
                    "User {} - Update order for playlist {}: Processed client song set does not match DB song set. Client (unique, ordered): {}, DB: {}",
                    user.getUsername(), playlistId, uniqueOrderedSongIdsSet, dbSongIdsSet);
            ResponseUtils.sendError(response, HttpServletResponse.SC_BAD_REQUEST,
                    "Submitted song IDs (after removing duplicates) do not exactly match the songs currently in the playlist.");
            return Optional.empty();
        }

        for (Integer songId : uniqueOrderedSongIdsSet) {
            if (songId == null || songId <= 0) {
                logger.warn("User {} - Update order for playlist {}: Invalid song ID ({}) in processed set.",
                        user.getUsername(), playlistId, songId);
                ResponseUtils.sendError(response, HttpServletResponse.SC_BAD_REQUEST,
                        "Invalid song ID provided: " + songId + ". All song IDs must be positive integers.");
                return Optional.empty();
            }
        }
        return Optional.of(new ArrayList<>(uniqueOrderedSongIdsSet));
    }

    private void handleUpdatePlaylistOrder(HttpServletRequest request, HttpServletResponse response, User user,
            String playlistIdStr) {
        logger.debug("User {} attempting to update order for playlist ID string: {}", user.getUsername(),
                playlistIdStr);

        Optional<Integer> playlistIdOpt = parseAndValidatePlaylistId(playlistIdStr, response, user);
        if (playlistIdOpt.isEmpty()) {
            return;
        }
        int playlistId = playlistIdOpt.get();
        logger.debug("User {} attempting to update order for playlist ID: {}", user.getUsername(), playlistId);

        Optional<Playlist> dbPlaylistOpt = verifyPlaylistOwnershipAndExistence(playlistId, user, response);
        if (dbPlaylistOpt.isEmpty()) {
            return;
        }
        Playlist dbPlaylist = dbPlaylistOpt.get();

        Optional<List<Integer>> rawClientSongIdsOpt = parseSongIdsFromRequest(request, response, user, playlistId);
        if (rawClientSongIdsOpt.isEmpty()) {
            return;
        }
        List<Integer> rawClientSongIds = rawClientSongIdsOpt.get();

        Optional<List<Integer>> validatedSongOrderOpt = processAndValidateSongOrder(rawClientSongIds, dbPlaylist,
                response, user, playlistId);
        if (validatedSongOrderOpt.isEmpty()) {
            return;
        }
        List<Integer> validatedSongOrder = validatedSongOrderOpt.get();

        try {
            playlistOrderDAO.savePlaylistOrder(playlistId, validatedSongOrder);
            ResponseUtils.sendJson(response, HttpServletResponse.SC_OK, validatedSongOrder);
            logger.info("User {} successfully updated order for playlist {}.", user.getUsername(), playlistId);
        } catch (DAOException e) {
            logger.error("User {} - DAOException while saving playlist order for playlist {}: {}", user.getUsername(),
                    playlistId, e.getMessage(), e);
            ResponseUtils.sendError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Error saving playlist order.");
        }
    }

    private void handleCreatePlaylist(HttpServletRequest request, HttpServletResponse response, User user) {
        PlaylistCreationRequest playlistRequest;
        try {
            playlistRequest = ObjectMapperUtils.getMapper().readValue(request.getInputStream(),
                    PlaylistCreationRequest.class);
        } catch (JsonProcessingException e) {
            logger.warn("Error parsing JSON for creating playlist by user {}: {}", user.getUsername(), e.getMessage());
            ResponseUtils.sendError(response, HttpServletResponse.SC_BAD_REQUEST,
                    "Invalid JSON format: " + e.getOriginalMessage());
            return;
        } catch (IOException e) {
            logger.error("IOException reading request body for creating playlist by user {}: {}", user.getUsername(),
                    e.getMessage(), e);
            ResponseUtils.sendError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Error reading request data.");
            return;
        }

        if (!isValidPlaylistCreationRequest(playlistRequest, response)) {
            return;
        }

        String name = playlistRequest.getName().trim();
        List<Integer> songIds = playlistRequest.getSongIds();

        try {
            Playlist createdPlaylist = playlistDAO.createPlaylist(name, user.getIdUser(), songIds);
            ResponseUtils.sendJson(response, HttpServletResponse.SC_CREATED, createdPlaylist);
            logger.info("User {} created playlist '{}' (ID: {}) successfully.", user.getUsername(),
                    createdPlaylist.getName(), createdPlaylist.getIdPlaylist());
        } catch (DAOException e) {
            if (e.getErrorType() == DAOException.DAOErrorType.GENERIC_ERROR) {
                logger.error("Generic DAOException during playlist creation for user {}: Type={}, Message={}",
                        user.getUsername(), e.getErrorType(), e.getMessage(), e);
            } else {
                logger.warn("DAOException during playlist creation for user {}: Type={}, Message={}",
                        user.getUsername(), e.getErrorType(), e.getMessage());
            }
            int statusCode = switch (e.getErrorType()) {
            case NAME_ALREADY_EXISTS -> HttpServletResponse.SC_CONFLICT;
            case NOT_FOUND, CONSTRAINT_VIOLATION, DUPLICATE_ENTRY -> HttpServletResponse.SC_BAD_REQUEST;
            default -> HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
            };
            ResponseUtils.sendError(response, statusCode, e.getMessage());
        } catch (Exception e) {
            logger.error("Unexpected error during playlist creation for user {}: {}", user.getUsername(),
                    e.getMessage(), e);
            ResponseUtils.sendError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "An unexpected error occurred while creating the playlist.");
        }
    }

    private void handleAddSongsToPlaylist(HttpServletRequest request, HttpServletResponse response, User user,
            String playlistIdStr) {
        int playlistId;
        try {
            playlistId = Integer.parseInt(playlistIdStr);
        } catch (NumberFormatException e) {
            logger.warn("Invalid playlist ID format '{}' in POST (add songs) for user {}.", playlistIdStr,
                    user.getUsername());
            ResponseUtils.sendError(response, HttpServletResponse.SC_BAD_REQUEST, "Invalid playlist ID format.");
            return;
        }

        PlaylistAddSongsRequest addSongsRequest;
        try {
            addSongsRequest = ObjectMapperUtils.getMapper().readValue(request.getInputStream(),
                    PlaylistAddSongsRequest.class);
        } catch (JsonProcessingException e) {
            logger.warn("Error parsing JSON for adding songs to playlist {} by user {}: {}", playlistId,
                    user.getUsername(), e.getMessage());
            ResponseUtils.sendError(response, HttpServletResponse.SC_BAD_REQUEST,
                    "Invalid JSON format: " + e.getOriginalMessage());
            return;
        } catch (IOException e) {
            logger.error("IOException reading request body for adding songs to playlist {} by user {}: {}", playlistId,
                    user.getUsername(), e.getMessage(), e);
            ResponseUtils.sendError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Error reading request data.");
            return;
        }

        List<Integer> songIds = addSongsRequest.getSongIds();
        if (songIds == null || songIds.isEmpty()) {
            ResponseUtils.sendError(response, HttpServletResponse.SC_BAD_REQUEST,
                    "songIds array is required and cannot be empty.");
            return;
        }
        for (Integer songId : songIds) {
            if (songId == null || songId <= 0) {
                ResponseUtils.sendError(response, HttpServletResponse.SC_BAD_REQUEST,
                        "Invalid song ID provided: " + songId + ". All song IDs must be positive integers.");
                return;
            }
        }
        if (songIds.size() > 500) {
            ResponseUtils.sendError(response, HttpServletResponse.SC_BAD_REQUEST,
                    "Cannot process more than 500 song IDs at once.");
            return;
        }

        try {
            AddSongsToPlaylistResult result = playlistDAO.addSongsToPlaylist(playlistId, user.getIdUser(), songIds);

            Map<String, Object> successResponse = new HashMap<>();
            successResponse.put("message", "Songs processed for playlist " + playlistId + ".");
            successResponse.put("addedSongIds", result.getAddedSongIds());
            successResponse.put("duplicateSongIds", result.getDuplicateSongIds());

            ResponseUtils.sendJson(response, HttpServletResponse.SC_OK, successResponse);
            logger.info("User {} processed adding songs to playlist {}. Added: {}, Duplicates: {}", user.getUsername(),
                    playlistId, result.getAddedSongIds().size(), result.getDuplicateSongIds().size());

        } catch (DAOException e) {
            if (e.getErrorType() == DAOException.DAOErrorType.GENERIC_ERROR) {
                logger.error("Generic DAOException while adding songs to playlist {} for user {}: Type={}, Message={}",
                        playlistId, user.getUsername(), e.getErrorType(), e.getMessage(), e);
            } else {
                logger.warn("DAOException while adding songs to playlist {} for user {}: Type={}, Message={}",
                        playlistId, user.getUsername(), e.getErrorType(), e.getMessage());
            }
            switch (e.getErrorType()) {
            case NOT_FOUND:
                ResponseUtils.sendError(response, HttpServletResponse.SC_NOT_FOUND, e.getMessage());
                break;
            case ACCESS_DENIED:
                ResponseUtils.sendError(response, HttpServletResponse.SC_FORBIDDEN, e.getMessage());
                break;
            case CONSTRAINT_VIOLATION:
                ResponseUtils.sendError(response, HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
                break;
            default:
                ResponseUtils.sendError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                        "Error adding songs to playlist: " + e.getMessage());
                break;
            }
        }
    }

    private boolean isValidPlaylistCreationRequest(PlaylistCreationRequest requestData, HttpServletResponse response) {
        String name = requestData.getName();
        if (name == null) {
            ResponseUtils.sendError(response, HttpServletResponse.SC_BAD_REQUEST, "Playlist name is required.");
            return false;
        }
        String trimmedName = name.trim();

        // Check if validation rules are loaded, fail if critical ones are missing
        if (playlistNameMinLength == null || playlistNameMaxLength == null || playlistNamePattern == null) {
            logger.error(
                    "CRITICAL: One or more playlist name validation rules not loaded from context. playlistNameMinLength: {}, playlistNameMaxLength: {}, playlistNamePattern: {}. Aborting playlist creation.",
                    playlistNameMinLength, playlistNameMaxLength, playlistNamePattern);
            ResponseUtils.sendServiceUnavailableError(response,
                    "Server configuration error preventing playlist validation.");
            return false;
        }

        if (trimmedName.isEmpty()) {
            ResponseUtils.sendError(response, HttpServletResponse.SC_BAD_REQUEST, "Playlist name cannot be empty.");
            return false;
        }
        if (trimmedName.length() < playlistNameMinLength) {
            ResponseUtils.sendError(response, HttpServletResponse.SC_BAD_REQUEST,
                    "Playlist name must be at least " + playlistNameMinLength + " characters.");
            return false;
        }
        if (trimmedName.length() > playlistNameMaxLength) {
            ResponseUtils.sendError(response, HttpServletResponse.SC_BAD_REQUEST,
                    "Playlist name cannot exceed " + playlistNameMaxLength + " characters.");
            return false;
        }
        if (!playlistNamePattern.matcher(trimmedName).matches()) {
            ResponseUtils.sendError(response, HttpServletResponse.SC_BAD_REQUEST,
                    "Playlist name contains invalid characters or does not meet format requirements.");
            return false;
        }

        List<Integer> songIds = requestData.getSongIds();
        if (songIds != null) {
            if (songIds.size() > 500) {
                ResponseUtils.sendError(response, HttpServletResponse.SC_BAD_REQUEST,
                        "Cannot add more than 500 songs at once to a new playlist.");
                return false;
            }
            // Use a stream to find the first invalid songId
            Optional<Integer> firstInvalidId = songIds.stream().filter(songId -> songId == null || songId <= 0)
                    .findFirst();

            if (firstInvalidId.isPresent()) {
                ResponseUtils.sendError(response, HttpServletResponse.SC_BAD_REQUEST, "Invalid song ID provided: "
                        + firstInvalidId.get() + ". All song IDs must be positive integers.");
                return false;
            }
        }
        return true;
    }

    @Override
    public void destroy() {
        try {
            if (connection != null && !connection.isClosed()) {
                ConnectionHandler.closeConnection(connection);
                logger.info("Database connection closed for PlaylistApiServlet.");
            }
        } catch (SQLException e) {
            logger.error("Failed to close database connection in destroy()", e);
        }
        super.destroy();
        logger.info("PlaylistApiServlet destroyed.");
    }

    private enum PlaylistActionRoute {
        // GET actions
        GET_USER_PLAYLISTS, GET_PLAYLIST_ORDER,

        // POST actions
        CREATE_PLAYLIST, ADD_SONGS_TO_PLAYLIST,

        // PUT actions
        UPDATE_PLAYLIST_ORDER,

        INVALID_ROUTE
    }
}
