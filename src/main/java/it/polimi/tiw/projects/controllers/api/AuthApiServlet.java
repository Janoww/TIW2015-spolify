package it.polimi.tiw.projects.controllers.api;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.polimi.tiw.projects.beans.LoginRequest;
import it.polimi.tiw.projects.beans.User;
import it.polimi.tiw.projects.dao.UserDAO;
import it.polimi.tiw.projects.exceptions.DAOException;
import it.polimi.tiw.projects.listeners.AppContextListener;
import it.polimi.tiw.projects.utils.ConnectionHandler;
import it.polimi.tiw.projects.utils.ResponseUtils;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@WebServlet("/api/v1/auth/*")
public class AuthApiServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final Logger logger = LoggerFactory.getLogger(AuthApiServlet.class);
    private transient Connection connection;

    public AuthApiServlet() {
        super();
    }

    @Override
    public void init() throws ServletException {
        ServletContext context = getServletContext();
        connection = ConnectionHandler.getConnection(context);
        logger.info("AuthApiServlet initialized.");
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String pathInfo = req.getPathInfo();
        String action = (pathInfo != null && pathInfo.equals("/me")) ? "CheckSession" : "Unknown";
        logger.info("Received GET request. Path: '{}', Action: {}", (pathInfo != null ? pathInfo : ""), action);

        if ("/me".equals(pathInfo)) {
            handleCheckSession(req, resp);
        } else {
            logger.warn("Invalid path for GET request: /api/v1/auth{}", (pathInfo != null ? pathInfo : ""));
            ResponseUtils.sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Endpoint not found.");
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String pathInfo = req.getPathInfo();
        logger.info("Received POST request. Path: '{}'", (pathInfo != null ? pathInfo : ""));

        if ("/login".equals(pathInfo)) {
            handleLogin(req, resp);
        } else if ("/logout".equals(pathInfo)) {
            handleLogout(req, resp);
        } else {
            logger.warn("Invalid path for POST request: /api/v1/auth{}", (pathInfo != null ? pathInfo : ""));
            ResponseUtils.sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Endpoint not found.");
        }
    }

    private void handleLogin(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        logger.debug("Handling login request.");
        UserDAO userDAO = new UserDAO(connection);
        LoginRequest loginDetails;

        try {
            // Assuming LoginRequest.class is appropriate for the JSON structure
            loginDetails = new ObjectMapper().readValue(req.getReader(), LoginRequest.class);
        } catch (JsonParseException | MismatchedInputException e) {
            logger.warn("Failed to parse JSON request body for login: {}", e.getMessage());
            ResponseUtils.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Invalid JSON format or missing fields.");
            return;
        }

        String username = loginDetails.getUsername() != null ? loginDetails.getUsername().strip() : null;
        String password = loginDetails.getPassword();

        // OWASP: Input Validation - Username and Password presence
        if (username == null || username.isEmpty() || password == null || password.isEmpty()) {
            logger.warn("Login attempt with missing credentials for username: {}",
                    (username != null ? username : "null"));
            ResponseUtils.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Missing username or password.");
            return;
        }

        ServletContext servletContext = getServletContext();
        Pattern usernamePattern = (Pattern) servletContext.getAttribute(AppContextListener.USERNAME_REGEX_PATTERN);
        Integer passwordMinLength = (Integer) servletContext.getAttribute(AppContextListener.PASSWORD_MIN_LENGTH);
        Integer passwordMaxLength = (Integer) servletContext.getAttribute(AppContextListener.PASSWORD_MAX_LENGTH);

        // Check for missing servlet context parameters
        if (usernamePattern == null || passwordMinLength == null || passwordMaxLength == null) {
            logger.error(
                    "CRITICAL: One or more servlet context validation parameters are null. usernamePattern: {}, passwordMinLength: {}, passwordMaxLength: {}. Aborting login.",
                    usernamePattern, passwordMinLength, passwordMaxLength);
            ResponseUtils.sendServiceUnavailableError(resp, "Server configuration error preventing login validation.");
            return;
        }

        // Username format
        if (!usernamePattern.matcher(username).matches()) {
            logger.warn("Login attempt with invalid username format (context-configured regex): {}", username);
            ResponseUtils.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Invalid username format.");
            return;
        }

        // Password minimum length
        if (password.length() < passwordMinLength) {
            logger.warn("Login attempt with password too short (context-configured min length: {}) for username: {}",
                    passwordMinLength, username);
            ResponseUtils.sendError(resp, HttpServletResponse.SC_BAD_REQUEST,
                    "Password must be at least " + passwordMinLength + " characters long.");
            return;
        }
        // Password maximum length
        if (password.length() > passwordMaxLength) {
            logger.warn("Login attempt with password too long (context-configured max length: {}) for username: {}",
                    passwordMaxLength, username);
            ResponseUtils.sendError(resp, HttpServletResponse.SC_BAD_REQUEST,
                    "Password must be at most " + passwordMaxLength + " characters long.");
            return;
        }

        User user;
        try {
            user = userDAO.checkCredentials(username, password);

            logger.info("User {} successfully authenticated.", username);
        } catch (DAOException e) {
            if (e.getErrorType() == DAOException.DAOErrorType.INVALID_CREDENTIALS) {
                logger.warn("Invalid credentials attempt for username: {}. Details: {}", username, e.getMessage());
                ResponseUtils.sendError(resp, HttpServletResponse.SC_UNAUTHORIZED, "Invalid username or password.");
            } else {
                logger.warn("DAOException during login for username: {}. ErrorType: {}", username, e.getErrorType(), e);
                ResponseUtils.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                        "Login failed due to a server error.");
            }
            return;
        }

        // Prevent session fixation: invalidate old session and create a new one
        HttpSession oldSession = req.getSession(false);
        if (oldSession != null) {
            logger.debug("Invalidating old session {} before creating a new one for user {}", oldSession.getId(),
                    username);
            oldSession.invalidate();
        }
        HttpSession newSession = req.getSession(true);
        newSession.setAttribute("user", user);
        logger.debug("User {} (ID: {}) set in new session {}.", user.getUsername(), user.getIdUser(),
                newSession.getId());

        Map<String, String> userResponse = new HashMap<>();
        userResponse.put("username", user.getUsername());
        userResponse.put("name", user.getName());
        userResponse.put("surname", user.getSurname());

        ResponseUtils.sendJson(resp, HttpServletResponse.SC_OK, userResponse);
        logger.debug("Successfully sent OK response with user details for user: {}", user.getUsername());
    }

    private void handleCheckSession(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        logger.debug("Handling check session request (/me).");
        HttpSession session = req.getSession(false);

        if (session != null) {
            User user = (User) session.getAttribute("user");
            if (user != null) {
                logger.info("Active session found for user: {}", user.getUsername());
                Map<String, String> userResponse = new HashMap<>();
                userResponse.put("username", user.getUsername());
                userResponse.put("name", user.getName());
                userResponse.put("surname", user.getSurname());

                ResponseUtils.sendJson(resp, HttpServletResponse.SC_OK, userResponse);
                logger.debug("Successfully sent OK response with user details for active session: {}",
                        user.getUsername());
                return;
            } else {
                logger.info("Session exists but no user attribute found.");
            }
        } else {
            logger.info("No active session found for /me request.");
        }

        // If we reach here, no active user session was found
        ResponseUtils.sendError(resp, HttpServletResponse.SC_UNAUTHORIZED,
                "No active session or user not authenticated.");
        logger.debug("Sent UNAUTHORIZED response for /me request.");
    }

    private void handleLogout(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        logger.debug("Handling logout request.");
        HttpSession session = req.getSession(false);
        if (session != null) {
            User user = (User) session.getAttribute("user");
            if (user != null) {
                logger.info("Logging out user: {}", user.getUsername());
            } else {
                logger.info("Logging out session without user attribute.");
            }
            session.invalidate();
        } else {
            logger.debug("Logout request for a session that does not exist or is already invalidated.");
        }
        Map<String, String> successResponse = new HashMap<>();
        successResponse.put("message", "Logout successful.");
        ResponseUtils.sendJson(resp, HttpServletResponse.SC_OK, successResponse);
        logger.debug("Logout successful response sent.");
    }

    @Override
    public void destroy() {
        try {
            ConnectionHandler.closeConnection(connection);
            logger.info("AuthApiServlet destroyed. Connection closed.");
        } catch (SQLException e) {
            logger.error("Failed to close database connection on servlet destroy.", e);
        }
    }
}
