package it.polimi.tiw.projects.controllers.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import it.polimi.tiw.projects.beans.LoginRequest;
import it.polimi.tiw.projects.beans.User;
import it.polimi.tiw.projects.dao.UserDAO;
import it.polimi.tiw.projects.exceptions.DAOException;
import it.polimi.tiw.projects.listeners.AppContextListener;
import it.polimi.tiw.projects.utils.ConnectionHandler;
import it.polimi.tiw.projects.utils.ObjectMapperUtils;
import it.polimi.tiw.projects.utils.ResponseUtils;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
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
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

@WebServlet("/api/v1/auth/*")
public class AuthApiServlet extends HttpServlet {
    @Serial
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

    private AuthRoute resolveRoute(HttpServletRequest request) {
        String method = request.getMethod();
        String pathInfo = request.getPathInfo();

        if ("GET".equalsIgnoreCase(method)) {
            Pattern mePattern = Pattern.compile("^/me/?$");
            if (pathInfo != null && mePattern.matcher(pathInfo).matches()) {
                return AuthRoute.CHECK_SESSION;
            }
        } else if ("POST".equalsIgnoreCase(method)) {
            Pattern loginPattern = Pattern.compile("^/login/?$");
            if (pathInfo != null && loginPattern.matcher(pathInfo).matches()) {
                return AuthRoute.LOGIN;
            }
            Pattern logoutPattern = Pattern.compile("^/logout/?$");
            if (pathInfo != null && logoutPattern.matcher(pathInfo).matches()) {
                return AuthRoute.LOGOUT;
            }
        }
        return AuthRoute.INVALID_ROUTE;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) {
        AuthRoute route = resolveRoute(req);
        String pathInfo = req.getPathInfo();
        logger.info("Received GET request. Path: '{}', Route: {}", (pathInfo != null ? pathInfo : "null or empty"),
                route);

        if (route != AuthRoute.CHECK_SESSION) {
            logger.warn("Invalid path for GET request: /api/v1/auth{}", (pathInfo != null ? pathInfo : ""));
            ResponseUtils.sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Endpoint not found.");
            return;
        }

        handleCheckSession(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) {
        AuthRoute route = resolveRoute(req);
        String pathInfo = req.getPathInfo();
        logger.info("Received POST request. Path: '{}', Route: {}", (pathInfo != null ? pathInfo : "null or empty"),
                route);

        switch (route) {
            case LOGIN:
                handleLogin(req, resp);
                break;
            case LOGOUT:
                handleLogout(req, resp);
                break;
            default:
                logger.warn("Invalid path for POST request: /api/v1/auth{}", (pathInfo != null ? pathInfo : ""));
                ResponseUtils.sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Endpoint not found.");
                break;
        }
    }

    private void handleLogin(HttpServletRequest req, HttpServletResponse resp) {
        logger.debug("Handling login request.");
        UserDAO userDAO = new UserDAO(connection);
        LoginRequest loginDetails;

        try {
            loginDetails = ObjectMapperUtils.getMapper().readValue(req.getReader(), LoginRequest.class);
        } catch (JsonProcessingException e) {
            logger.warn("Failed to parse JSON request body for login: {}", e.getMessage());
            ResponseUtils.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Invalid JSON format or missing fields.");
            return;
        } catch (IOException e) {
            logger.warn("Failed to read request body for login: {}", e.getMessage());
            ResponseUtils.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error reading request data.");
            return;
        }

        String username = loginDetails.getUsername();
        String password = loginDetails.getPassword();

        // OWASP: Input Validation - Username and Password presence
        if (username == null || username.isEmpty() || password == null || password.isEmpty()) {
            logger.warn("Login attempt with missing credentials for username: {}",
                    (username != null ? username : "null"));
            ResponseUtils.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Missing username or password.");
            return;
        }

        username = username.strip();

        try {
            validateLoginCredentials(req, username, password);
        } catch (IllegalArgumentException e) {
            ResponseUtils.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
            return;
        } catch (IllegalStateException e) {
            ResponseUtils.sendServiceUnavailableError(resp, e.getMessage());
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

    private void validateLoginCredentials(HttpServletRequest req, String username, String password)
            throws IllegalArgumentException, IllegalStateException {
        ServletContext servletContext = req.getServletContext();
        Pattern usernamePattern = (Pattern) servletContext.getAttribute(AppContextListener.USERNAME_REGEX_PATTERN);
        Integer passwordMinLength = (Integer) servletContext.getAttribute(AppContextListener.PASSWORD_MIN_LENGTH);
        Integer passwordMaxLength = (Integer) servletContext.getAttribute(AppContextListener.PASSWORD_MAX_LENGTH);

        // Check for missing servlet context parameters
        if (usernamePattern == null || passwordMinLength == null || passwordMaxLength == null) {
            logger.error(
                    "CRITICAL: One or more servlet context validation parameters are null. usernamePattern: {}, passwordMinLength: {}, passwordMaxLength: {}. Aborting login.",
                    usernamePattern, passwordMinLength, passwordMaxLength);
            throw new IllegalStateException("Server configuration error preventing login validation.");
        }

        // Username format
        if (!usernamePattern.matcher(username).matches()) {
            logger.warn("Login attempt with invalid username format (context-configured regex): {}", username);
            throw new IllegalArgumentException("Invalid username format.");
        }

        // Password minimum length
        if (password.length() < passwordMinLength) {
            logger.warn("Login attempt with password too short (context-configured min length: {}) for username: {}",
                    passwordMinLength, username);
            throw new IllegalArgumentException("Password must be at least " + passwordMinLength + " characters long.");
        }

        // Password maximum length
        if (password.length() > passwordMaxLength) {
            logger.warn("Login attempt with password too long (context-configured max length: {}) for username: {}",
                    passwordMaxLength, username);
            throw new IllegalArgumentException("Password must be at most " + passwordMaxLength + " characters long.");
        }
    }

    private void handleCheckSession(HttpServletRequest req, HttpServletResponse resp) {
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

    private void handleLogout(HttpServletRequest req, HttpServletResponse resp) {
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

    private enum AuthRoute {
        LOGIN, LOGOUT, CHECK_SESSION, INVALID_ROUTE
    }
}
