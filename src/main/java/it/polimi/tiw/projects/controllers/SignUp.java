package it.polimi.tiw.projects.controllers;

import it.polimi.tiw.projects.beans.User;
import it.polimi.tiw.projects.dao.UserDAO;
import it.polimi.tiw.projects.exceptions.DAOException;
import it.polimi.tiw.projects.listeners.AppContextListener;
import it.polimi.tiw.projects.utils.ConnectionHandler;
import it.polimi.tiw.projects.utils.TemplateHandler;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.WebContext;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Objects;
import java.util.regex.Pattern;

public class SignUp extends HttpServlet {
    private static final Logger logger = LoggerFactory.getLogger(SignUp.class);
    private static final long serialVersionUID = 1L;
    private Connection connection;
    private TemplateEngine templateEngine;

    public SignUp() {
        super();
    }

    private static boolean isValid(@NotNull String parameter, @NotNull Pattern pattern) {
        return !pattern.matcher(parameter).matches();
    }

    private static boolean validLengthConstraints(@NotNull Integer min, @NotNull Integer max, @NotNull String string) {
        return string.length() <= max && string.length() >= min;
    }

    @Override
    public void init() throws ServletException {
        ServletContext context = getServletContext();
        connection = ConnectionHandler.getConnection(context);
        templateEngine = TemplateHandler.initializeEngine(context);

    }

    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        UserDAO userDAO = new UserDAO(connection);

        String name = req.getParameter("sName").strip();
        String surname = req.getParameter("sSurname").strip();
        String username = req.getParameter("sUsername").strip();
        String password = req.getParameter("sPwd").strip();

        WebContext ctx = TemplateHandler.getWebContext(req, resp, getServletContext());

        // Checking that parameters are not empty
        if (name == null || surname == null || username == null || password == null || name.isEmpty()
                || surname.isEmpty() || password.isEmpty() || username.isEmpty()) {
            logger.warn("Missing credential value");
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing credential value");
            return;
        }

        // Input validation
        ServletContext servletContext = getServletContext();
        Pattern namePattern = (Pattern) servletContext.getAttribute(AppContextListener.NAME_REGEX_PATTERN);
        Pattern usernamePattern = (Pattern) servletContext.getAttribute(AppContextListener.USERNAME_REGEX_PATTERN);
        Integer passwordMinLength = (Integer) servletContext.getAttribute(AppContextListener.PASSWORD_MIN_LENGTH);
        Integer passwordMaxLength = (Integer) servletContext.getAttribute(AppContextListener.PASSWORD_MAX_LENGTH);

        if (isValid(name, namePattern)) {
            logger.warn("Invalid name format. Use letters, spaces, hyphens, or apostrophes (3-100 characters).");
            ctx.setVariable("errorSignUpMsg",
                    "Invalid name format. Use letters, spaces, hyphens, or apostrophes (3-100 characters).");
            String path = "/index.html";
            templateEngine.process(path, ctx, resp.getWriter());
            return;
        }
        if (isValid(surname, namePattern)) {
            logger.warn("Invalid surname format. Use letters, spaces, hyphens, or apostrophes (3-100 characters).");
            ctx.setVariable("errorSignUpMsg",
                    "Invalid surname format. Use letters, spaces, hyphens, or apostrophes (3-100 characters).");
            String path = "/index.html";
            templateEngine.process(path, ctx, resp.getWriter());
            return;
        }
        if (isValid(username, usernamePattern)) {
            logger.warn("Invalid username format. Use alphanumeric characters or underscores (3-100 characters).");
            ctx.setVariable("errorSignUpMsg",
                    "Invalid username format. Use alphanumeric characters or underscores (3-100 characters).");
            String path = "/index.html";
            templateEngine.process(path, ctx, resp.getWriter());
            return;
        }
        if (!validLengthConstraints(passwordMinLength, passwordMaxLength, password)) {
            logger.warn("Passowrd length must be between " + passwordMinLength + " and " + passwordMaxLength
                    + " characters");
            ctx.setVariable("errorSignUpMsg", "Passowrd length must be between " + passwordMinLength + " and "
                    + passwordMaxLength + " characters");
            String path = "/index.html";
            templateEngine.process(path, ctx, resp.getWriter());
            return;
        }

        // Try to create a new user
        try {
            userDAO.createUser(username, password, name, surname);
        } catch (DAOException e) {
            if (Objects.requireNonNull(e.getErrorType()) == DAOException.DAOErrorType.NAME_ALREADY_EXISTS) {// If a user with that name already exists:
                logger.warn("Username already taken");
                ctx.setVariable("errorSignUpMsg", "Username already taken");
                String path = "/index.html";
                templateEngine.process(path, ctx, resp.getWriter());
            } else {// If another exception occurs
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Not possible to sign up");
            }
            return;
        }

        // If we are here the user was created successfully, let's retrieve it
        User user = null;
        try {
            user = userDAO.checkCredentials(username, password);
        } catch (DAOException e) { // Failed to retrieve the newly created user
            e.printStackTrace();
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Not Possible to check credentials");
            return;
        }

        // Assign the user to the session
        req.getSession().setAttribute("user", user);
        String path = getServletContext().getContextPath() + "/Home";
        resp.sendRedirect(path);
    }

    @Override
    public void destroy() {
        try {
            ConnectionHandler.closeConnection(connection);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
