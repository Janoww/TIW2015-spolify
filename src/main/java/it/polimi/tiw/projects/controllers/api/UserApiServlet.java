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

import it.polimi.tiw.projects.beans.User;
import it.polimi.tiw.projects.beans.UserCreationRequest;
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

@WebServlet("/api/v1/users/*")
public class UserApiServlet extends HttpServlet {
        private static final long serialVersionUID = 1L;
        private static final Logger logger = LoggerFactory.getLogger(UserApiServlet.class);
        private Connection connection;

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
        protected void doPost(HttpServletRequest req, HttpServletResponse resp)
                        throws ServletException, IOException {
                logger.debug("Received POST request to /api/v1/users for user sign up.");
                UserDAO userDAO = new UserDAO(connection);
                UserCreationRequest userCreationDetails;

                try {
                        userCreationDetails = new ObjectMapper().readValue(req.getReader(),
                                        UserCreationRequest.class);
                } catch (JsonParseException | MismatchedInputException e) {
                        logger.warn("Failed to parse JSON request body for user sign up: {}",
                                        e.getMessage());
                        ResponseUtils.sendError(resp, HttpServletResponse.SC_BAD_REQUEST,
                                        "Invalid JSON format or missing fields.");
                        return;
                }

                String name = userCreationDetails.getName() != null
                                ? userCreationDetails.getName().strip()
                                : null;
                String surname = userCreationDetails.getSurname() != null
                                ? userCreationDetails.getSurname().strip()
                                : null;
                String username = userCreationDetails.getUsername() != null
                                ? userCreationDetails.getUsername().strip()
                                : null;
                String password = userCreationDetails.getPassword();

                logger.debug("Attempting to sign up user with username: {}", username);

                // OWASP: Input Validation - Check for presence of all fields
                if (name == null || name.isEmpty() || surname == null || surname.isEmpty()
                                || username == null || username.isEmpty() || password == null
                                || password.isEmpty()) {
                        logger.warn("Sign up attempt with missing credential values for username: {}",
                                        (username != null ? username : "null"));
                        ResponseUtils.sendError(resp, HttpServletResponse.SC_BAD_REQUEST,
                                        "Missing required fields: name, surname, username, password.");
                        return;
                }
                logger.debug("All parameters present for username: {}",
                                (username != null ? username : "null"));

                ServletContext servletContext = getServletContext();
                Pattern namePattern = (Pattern) servletContext
                                .getAttribute(AppContextListener.NAME_REGEX_PATTERN);
                Pattern usernamePattern = (Pattern) servletContext
                                .getAttribute(AppContextListener.USERNAME_REGEX_PATTERN);
                Integer passwordMinLength = (Integer) servletContext
                                .getAttribute(AppContextListener.PASSWORD_MIN_LENGTH);
                Integer passwordMaxLength = (Integer) servletContext
                                .getAttribute(AppContextListener.PASSWORD_MAX_LENGTH);

                if (namePattern != null) {
                        // Input Validation - Name format
                        if (!namePattern.matcher(name).matches()) {
                                logger.warn("Sign up attempt with invalid name format (context-configured regex): {}",
                                                name);
                                ResponseUtils.sendError(resp, HttpServletResponse.SC_BAD_REQUEST,
                                                "Invalid name format. Use letters, spaces, hyphens, or apostrophes (3-100 characters).");
                                return;
                        }
                        // Input Validation - Surname format
                        if (!namePattern.matcher(surname).matches()) {
                                logger.warn("Sign up attempt with invalid surname format (context-configured regex): {}",
                                                surname);
                                ResponseUtils.sendError(resp, HttpServletResponse.SC_BAD_REQUEST,
                                                "Invalid surname format. Use letters, spaces, hyphens, or apostrophes (3-100 characters).");
                                return;
                        }
                } else {
                        logger.error("Name regex pattern not available from servlet context. Name and surname validation might be incomplete.");
                }

                // Input Validation - Username format
                if (usernamePattern != null) {
                        if (!usernamePattern.matcher(username).matches()) {
                                logger.warn("Sign up attempt with invalid username format (context-configured regex): {}",
                                                username);
                                ResponseUtils.sendError(resp, HttpServletResponse.SC_BAD_REQUEST,
                                                "Invalid username format. Use alphanumeric characters or underscores (3-100 characters).");
                                return;
                        }
                } else {
                        logger.error("Username regex pattern not available from servlet context. Username validation might be incomplete.");
                }

                if (passwordMinLength != null && passwordMaxLength != null) {
                        // Input Validation - Password minimum length
                        if (password.length() < passwordMinLength) {
                                logger.warn("Sign up attempt with password too short (context-configured min length: {}) for username: {}",
                                                passwordMinLength, username);
                                ResponseUtils.sendError(resp, HttpServletResponse.SC_BAD_REQUEST,
                                                "Password must be at least " + passwordMinLength
                                                                + " characters long.");
                                return;
                        }
                        // Input Validation - Password maximum length
                        if (password.length() > passwordMaxLength) {
                                logger.warn("Sign up attempt with password too long (context-configured max length: {}) for username: {}",
                                                passwordMaxLength, username);
                                ResponseUtils.sendError(resp, HttpServletResponse.SC_BAD_REQUEST,
                                                "Password must be at most " + passwordMaxLength
                                                                + " characters long.");
                                return;
                        }
                } else {
                        logger.error("Password minimum length not available from servlet context. Password length validation might be incomplete.");
                }

                User createdUser;
                try {
                        userDAO.createUser(username, password, name, surname);
                        logger.info("Successfully initiated creation for user with username: {}",
                                        username);

                        createdUser = userDAO.checkCredentials(username, password);
                        logger.debug("Successfully retrieved newly created user: {}", username);

                } catch (DAOException e) {
                        if (e.getErrorType() == DAOException.DAOErrorType.NAME_ALREADY_EXISTS) {
                                logger.warn("Sign up attempt for already existing username: {}. Details: {}",
                                                username, e.getMessage());
                                ResponseUtils.sendError(resp, HttpServletResponse.SC_CONFLICT,
                                                "Username already taken");
                        } else {
                                logger.error("DAOException during user creation for username: {}. ErrorType: {}. Details: {}",
                                                username, e.getErrorType(), e.getMessage(), e);
                                ResponseUtils.sendError(resp,
                                                HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                                                "Not possible to sign up due to a server error.");
                        }
                        return;
                }

                // Assign the user to the session
                req.getSession().setAttribute("user", createdUser);
                logger.debug("User {} (ID: {}) set in session after sign up.",
                                createdUser.getUsername(), createdUser.getIdUser());

                // Send user details as JSON (excluding password)
                Map<String, String> userResponse = new HashMap<>();
                userResponse.put("username", createdUser.getUsername());
                userResponse.put("name", createdUser.getName());
                userResponse.put("surname", createdUser.getSurname());

                resp.setStatus(HttpServletResponse.SC_CREATED);
                resp.setContentType("application/json");
                resp.setCharacterEncoding("UTF-8");
                // resp.getWriter().write(mapper.writeValueAsString(userResponse)); // Old way
                ResponseUtils.sendJson(resp, HttpServletResponse.SC_CREATED, userResponse); // New
                                                                                            // way
                logger.debug("Successfully sent CREATED response with user details for user: {}",
                                createdUser.getUsername());
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
