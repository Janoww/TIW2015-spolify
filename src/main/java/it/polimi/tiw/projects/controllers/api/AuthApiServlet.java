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

import it.polimi.tiw.projects.beans.LoginRequest;
import it.polimi.tiw.projects.beans.User;
import it.polimi.tiw.projects.dao.UserDAO;
import it.polimi.tiw.projects.exceptions.DAOException;
import it.polimi.tiw.projects.utils.ConnectionHandler;
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
    private Connection connection;

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
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String pathInfo = req.getPathInfo();
        logger.debug("Received POST request to /api/v1/auth{}", (pathInfo != null ? pathInfo : ""));

        if ("/login".equals(pathInfo)) {
            handleLogin(req, resp);
        } else if ("/logout".equals(pathInfo)) {
            handleLogout(req, resp);
        } else {
            logger.warn("Invalid path for POST request: /api/v1/auth{}", (pathInfo != null ? pathInfo : ""));
            resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
            resp.setContentType("application/json");
            resp.setCharacterEncoding("UTF-8");
            ObjectMapper mapper = new ObjectMapper();
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Endpoint not found.");
            resp.getWriter().write(mapper.writeValueAsString(errorResponse));
        }
    }

    private void handleLogin(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        logger.debug("Handling login request.");
        UserDAO userDAO = new UserDAO(connection);
        ObjectMapper mapper = new ObjectMapper();
        LoginRequest loginDetails;

        try {
            loginDetails = mapper.readValue(req.getReader(), LoginRequest.class);
        } catch (JsonParseException | MismatchedInputException e) {
            logger.warn("Failed to parse JSON request body for login: {}", e.getMessage());
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.setContentType("application/json");
            resp.setCharacterEncoding("UTF-8");
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Invalid JSON format or missing fields.");
            resp.getWriter().write(mapper.writeValueAsString(errorResponse));
            return;
        }

        String username = loginDetails.getUsername() != null ? loginDetails.getUsername().strip() : null;
        String password = loginDetails.getPassword();

        if (username == null || username.isEmpty() || password == null || password.isEmpty()) {
            logger.warn("Login attempt with missing credentials for username: {}", username);
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.setContentType("application/json");
            resp.setCharacterEncoding("UTF-8");
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Missing username or password.");
            resp.getWriter().write(mapper.writeValueAsString(errorResponse));
            return;
        }

        User user;
        try {
            user = userDAO.checkCredentials(username, password);

            logger.info("User {} successfully authenticated.", username);
        } catch (DAOException e) {
            if (e.getErrorType() == DAOException.DAOErrorType.INVALID_CREDENTIALS) {
                logger.warn("Invalid credentials attempt for username: {}. Details: {}", username, e.getMessage());
                resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                Map<String, String> errorResponse = new HashMap<>();
                errorResponse.put("error", "Invalid username or password.");
                resp.setContentType("application/json");
                resp.setCharacterEncoding("UTF-8");
                resp.getWriter().write(mapper.writeValueAsString(errorResponse));
            } else {
                logger.warn("DAOException during login for username: {}. ErrorType: {}", username, e.getErrorType(), e);
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                Map<String, String> errorResponse = new HashMap<>();
                errorResponse.put("error", "Login failed due to a server error.");
                resp.setContentType("application/json");
                resp.setCharacterEncoding("UTF-8");
                resp.getWriter().write(mapper.writeValueAsString(errorResponse));
            }
            return;
        }

        req.getSession().setAttribute("user", user);
        logger.debug("User {} (ID: {}) set in session.", user.getUsername(), user.getIdUser());

        Map<String, String> userResponse = new HashMap<>();
        userResponse.put("username", user.getUsername());
        userResponse.put("name", user.getName());
        userResponse.put("surname", user.getSurname());

        resp.setStatus(HttpServletResponse.SC_OK);
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        resp.getWriter().write(mapper.writeValueAsString(userResponse));
        logger.debug("Successfully sent OK response with user details for user: {}", user.getUsername());
    }

    private void handleLogout(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        logger.debug("Handling logout request.");
        HttpSession session = req.getSession(false); // false == do not create new session if one does not exist
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
        resp.setStatus(HttpServletResponse.SC_OK);
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        ObjectMapper mapper = new ObjectMapper();
        Map<String, String> successResponse = new HashMap<>();
        successResponse.put("message", "Logout successful.");
        resp.getWriter().write(mapper.writeValueAsString(successResponse));
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
