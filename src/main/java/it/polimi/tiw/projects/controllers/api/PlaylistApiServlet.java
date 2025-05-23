package it.polimi.tiw.projects.controllers.api;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import it.polimi.tiw.projects.beans.Playlist;
import it.polimi.tiw.projects.beans.PlaylistCreationRequest;
import it.polimi.tiw.projects.beans.User;
import it.polimi.tiw.projects.dao.PlaylistDAO;
import it.polimi.tiw.projects.exceptions.DAOException;
import it.polimi.tiw.projects.listeners.AppContextListener;
import it.polimi.tiw.projects.utils.ConnectionHandler;
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

@WebServlet("/api/v1/playlists")
public class PlaylistApiServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final Logger logger = LoggerFactory.getLogger(PlaylistApiServlet.class);

    private transient Connection connection;
    private transient PlaylistDAO playlistDAO;
    private transient ObjectMapper objectMapper;

    // Validation parameters
    private transient Pattern playlistNamePattern;
    private transient Integer playlistNameMinLength;
    private transient Integer playlistNameMaxLength;

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
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());

        // Load validation patterns from ServletContext
        this.playlistNamePattern = (Pattern) servletContext
                .getAttribute(AppContextListener.PLAYLIST_NAME_REGEX_PATTERN);
        // Playlist name uses standard min/max length
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

        String contentType = request.getContentType();
        if (contentType == null || !contentType.toLowerCase().startsWith("application/json")) {
            ResponseUtils.sendError(response, HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE,
                    "Content-Type must be application/json.");
            return;
        }

        PlaylistCreationRequest playlistRequest;
        try {
            playlistRequest = objectMapper.readValue(request.getInputStream(), PlaylistCreationRequest.class);
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            logger.warn("Error parsing JSON request for user {}: {}", user.getUsername(), e.getMessage());
            ResponseUtils.sendError(response, HttpServletResponse.SC_BAD_REQUEST,
                    "Invalid JSON format: " + e.getOriginalMessage());
            return;
        } catch (IOException e) {
            logger.error("IOException reading request body for user {}: {}", user.getUsername(), e.getMessage(), e);
            ResponseUtils.sendError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Error reading request data.");
            return;
        }

        // Input Validation Call
        if (!isValidPlaylistRequest(playlistRequest, response)) {
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
            logger.warn("DAOException during playlist creation for user {}: Type={}, Message={}", user.getUsername(),
                    e.getErrorType(), e.getMessage());
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

    private boolean isValidPlaylistRequest(PlaylistCreationRequest requestData, HttpServletResponse response) {
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
            for (Integer songId : songIds) {
                if (songId == null || songId <= 0) {
                    ResponseUtils.sendError(response, HttpServletResponse.SC_BAD_REQUEST,
                            "Invalid song ID provided: " + songId + ". All song IDs must be positive integers.");
                    return false;
                }
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
}
