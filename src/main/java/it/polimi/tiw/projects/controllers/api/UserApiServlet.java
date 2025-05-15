package it.polimi.tiw.projects.controllers.api;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.polimi.tiw.projects.beans.User;
import it.polimi.tiw.projects.beans.UserCreationRequest;
import it.polimi.tiw.projects.dao.UserDAO;
import it.polimi.tiw.projects.exceptions.DAOException;
import it.polimi.tiw.projects.utils.ConnectionHandler;
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
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        logger.debug("Received POST request to /api/v1/users for user sign up.");
        UserDAO userDAO = new UserDAO(connection);
        ObjectMapper mapper = new ObjectMapper();
        UserCreationRequest userCreationDetails;

        try {
            userCreationDetails = mapper.readValue(req.getReader(), UserCreationRequest.class);
        } catch (JsonParseException | MismatchedInputException e) {
            logger.warn("Failed to parse JSON request body for user sign up: {}", e.getMessage());
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.setContentType("application/json");
            resp.setCharacterEncoding("UTF-8");
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Invalid JSON format or missing fields.");
            resp.getWriter().write(mapper.writeValueAsString(errorResponse));
            return;
        }

        String name = userCreationDetails.getName() != null ? userCreationDetails.getName().strip() : null;
        String surname = userCreationDetails.getSurname() != null ? userCreationDetails.getSurname().strip() : null;
        String username = userCreationDetails.getUsername() != null ? userCreationDetails.getUsername().strip() : null;
        String password = userCreationDetails.getPassword();

        logger.debug("Attempting to sign up user with username: {}", username);

        // Checking that parameters are not empty
        if (name == null || name.isEmpty() || surname == null || surname.isEmpty() || username == null
                || username.isEmpty() || password == null || password.isEmpty()) {
            logger.warn("Sign up attempt with missing credential values for username: {}", username);
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.setContentType("application/json");
            resp.setCharacterEncoding("UTF-8");
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Missing required fields: name, surname, username, password.");
            resp.getWriter().write(mapper.writeValueAsString(errorResponse));
            return;
        }
        logger.debug("All parameters present for username: {}", username);

        User createdUser;
        try {
            userDAO.createUser(username, password, name, surname);
            logger.info("Successfully initiated creation for user with username: {}", username);

            createdUser = userDAO.checkCredentials(username, password);
            logger.debug("Successfully retrieved newly created user: {}", username);

        } catch (DAOException e) {
            if (e.getErrorType() == DAOException.DAOErrorType.NAME_ALREADY_EXISTS) {
                logger.warn("Sign up attempt for already existing username: {}. Details: {}", username, e.getMessage());
                resp.setStatus(HttpServletResponse.SC_CONFLICT);
                resp.setContentType("application/json");
                resp.setCharacterEncoding("UTF-8");
                Map<String, String> errorResponse = new HashMap<>();
                errorResponse.put("error", "Username already taken");
                resp.getWriter().write(mapper.writeValueAsString(errorResponse));
            } else {
                logger.error("DAOException during user creation for username: {}. ErrorType: {}. Details: {}", username,
                        e.getErrorType(), e.getMessage(), e);
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                resp.setContentType("application/json");
                resp.setCharacterEncoding("UTF-8");
                Map<String, String> errorResponse = new HashMap<>();
                errorResponse.put("error", "Not possible to sign up due to a server error.");
                resp.getWriter().write(mapper.writeValueAsString(errorResponse));
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

        resp.setStatus(HttpServletResponse.SC_CREATED);
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        resp.getWriter().write(mapper.writeValueAsString(userResponse));
        logger.debug("Successfully sent CREATED response with user details for user: {}", createdUser.getUsername());
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
