package it.polimi.tiw.projects.controllers.api;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.Optional;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.polimi.tiw.projects.beans.User;
import it.polimi.tiw.projects.beans.UserCreationRequest;
import it.polimi.tiw.projects.dao.UserDAO;
import it.polimi.tiw.projects.exceptions.DAOException;
import it.polimi.tiw.projects.listeners.AppContextListener;
import it.polimi.tiw.projects.utils.ConnectionHandler;
import it.polimi.tiw.projects.utils.ObjectMapperUtils;
import it.polimi.tiw.projects.utils.ResponseUtils;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.validation.constraints.NotBlank;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet("/api/v1/users")
public class UserApiServlet extends HttpServlet {
        private static final long serialVersionUID = 1L;
        private static final Logger logger = LoggerFactory.getLogger(UserApiServlet.class);
        private transient Connection connection;

        public UserApiServlet() {
                super();
        }

        @Override
        public void init() throws ServletException {
                ServletContext context = getServletContext();
                connection = ConnectionHandler.getConnection(context);
                logger.info("UserApiServlet initialized.");
        }

        @Override
        protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
                logger.info("Received POST request to /api/v1/users (SignUp). PathInfo: {}", req.getPathInfo());
                UserDAO userDAO = new UserDAO(connection);
                UserCreationRequest userCreationDetails;

                try {
                        userCreationDetails = ObjectMapperUtils.getMapper().readValue(req.getReader(),
                                        UserCreationRequest.class);
                } catch (JsonProcessingException e) {
                        logger.warn("Failed to parse JSON request body for user sign up: {}", e.getMessage());
                        ResponseUtils.sendError(resp, HttpServletResponse.SC_BAD_REQUEST,
                                        "Invalid JSON format or missing fields.");
                        return;
                } catch (IOException e) {
                        logger.warn("IOException occurred while parsing JSON request body for user sign up: {}",
                                        e.getMessage());
                        ResponseUtils.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
                        return;
                }

                String username = stripSafely(userCreationDetails.getUsername());
                String name = stripSafely(userCreationDetails.getName());
                String surname = stripSafely(userCreationDetails.getSurname());
                String password = userCreationDetails.getPassword();

                logger.debug("Attempting to sign up user with username: {}", username);

                // Input Validation - Check for presence of all fields
                if (Stream.of(name, surname, username, password).anyMatch(s -> s == null || s.isEmpty())) {
                        logger.warn("Sign up attempt with missing credential values for username: {}",
                                        (username != null ? username : "null"));
                        ResponseUtils.sendError(resp, HttpServletResponse.SC_BAD_REQUEST,
                                        "Missing required fields: name, surname, username, password.");
                        return;
                }
                logger.debug("All parameters present for username: {}", username);

                if (!validateUser(resp, username, name, surname, password))
                        return;

                User createdUser;
                try {
                        createdUser = userDAO.createUser(username, password, name, surname);
                        logger.info("User {} created and retrieved successfully with ID: {}", createdUser.getUsername(),
                                        createdUser.getIdUser());

                } catch (DAOException e) {
                        if (e.getErrorType() == DAOException.DAOErrorType.NAME_ALREADY_EXISTS) {
                                logger.warn("Sign up attempt for already existing username: {}. Details: {}", username,
                                                e.getMessage());
                                ResponseUtils.sendError(resp, HttpServletResponse.SC_CONFLICT,
                                                "Username already taken");
                        } else {
                                logger.error("DAOException during user creation for username: {}. ErrorType: {}. Details: {}",
                                                username, e.getErrorType(), e.getMessage(), e);
                                ResponseUtils.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                                                "Not possible to sign up due to a server error.");
                        }
                        return;
                }

                // Assign the user to the session
                req.getSession().setAttribute("user", createdUser);
                logger.debug("User {} (ID: {}) set in session after sign up.", createdUser.getUsername(),
                                createdUser.getIdUser());

                // Send user details as JSON (excluding password)
                Map<String, String> userResponse = new HashMap<>();
                userResponse.put("username", createdUser.getUsername());
                userResponse.put("name", createdUser.getName());
                userResponse.put("surname", createdUser.getSurname());

                ResponseUtils.sendJson(resp, HttpServletResponse.SC_CREATED, userResponse);
                logger.debug("Successfully sent CREATED response with user details for user: {}",
                                createdUser.getUsername());
        }

        private static String stripSafely(String s) {
                return Optional.ofNullable(s).map(String::strip).orElse(null);
        }

        private boolean validateUser(HttpServletResponse resp, @NotBlank String username, @NotBlank String name,
                        @NotBlank String surname, @NotBlank String password) {
                ServletContext servletContext = getServletContext();
                Pattern namePattern = (Pattern) servletContext.getAttribute(AppContextListener.NAME_REGEX_PATTERN);
                Pattern usernamePattern = (Pattern) servletContext
                                .getAttribute(AppContextListener.USERNAME_REGEX_PATTERN);
                Integer passwordMinLength = (Integer) servletContext
                                .getAttribute(AppContextListener.PASSWORD_MIN_LENGTH);
                Integer passwordMaxLength = (Integer) servletContext
                                .getAttribute(AppContextListener.PASSWORD_MAX_LENGTH);

                // Check for missing servlet context parameters
                if (namePattern == null || usernamePattern == null || passwordMinLength == null
                                || passwordMaxLength == null) {
                        logger.error("CRITICAL: One or more servlet context validation parameters are null. namePattern: {}, usernamePattern: {}, passwordMinLength: {}, passwordMaxLength: {}. Aborting user creation.",
                                        namePattern, usernamePattern, passwordMinLength, passwordMaxLength);
                        ResponseUtils.sendServiceUnavailableError(resp,
                                        "Server configuration error preventing user validation.");
                        return false;
                }

                // Validate Name and Surname
                if (!namePattern.matcher(name).matches()) {
                        logger.warn("Sign up attempt with invalid name format (context-configured regex): {}", name);
                        ResponseUtils.sendError(resp, HttpServletResponse.SC_BAD_REQUEST,
                                        "Invalid name format. Use letters, spaces, hyphens, or apostrophes (3-100 characters).");
                        return false;
                }
                if (!namePattern.matcher(surname).matches()) {
                        logger.warn("Sign up attempt with invalid surname format (context-configured regex): {}",
                                        surname);
                        ResponseUtils.sendError(resp, HttpServletResponse.SC_BAD_REQUEST,
                                        "Invalid surname format. Use letters, spaces, hyphens, or apostrophes (3-100 characters).");
                        return false;
                }

                // Validate Username
                if (!usernamePattern.matcher(username).matches()) {
                        logger.warn("Sign up attempt with invalid username format (context-configured regex): {}",
                                        username);
                        ResponseUtils.sendError(resp, HttpServletResponse.SC_BAD_REQUEST,
                                        "Invalid username format. Use alphanumeric characters or underscores (3-100 characters).");
                        return false;
                }

                // Validate Password Length
                if (password.length() < passwordMinLength) {
                        logger.warn("Sign up attempt with password too short (context-configured min length: {}) for username: {}",
                                        passwordMinLength, username);
                        ResponseUtils.sendError(resp, HttpServletResponse.SC_BAD_REQUEST,
                                        "Password must be at least " + passwordMinLength + " characters long.");
                        return false;
                }
                if (password.length() > passwordMaxLength) {
                        logger.warn("Sign up attempt with password too long (context-configured max length: {}) for username: {}",
                                        passwordMaxLength, username);
                        ResponseUtils.sendError(resp, HttpServletResponse.SC_BAD_REQUEST,
                                        "Password must be at most " + passwordMaxLength + " characters long.");
                        return false;
                }

                return true;
        }

        @Override
        public void destroy() {
                try {
                        ConnectionHandler.closeConnection(connection);
                        logger.info("UserApiServlet destroyed. Connection closed.");
                } catch (SQLException e) {
                        logger.error("Failed to close database connection on servlet destroy.", e);
                }
        }
}
