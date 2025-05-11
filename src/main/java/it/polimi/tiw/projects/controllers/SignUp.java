package it.polimi.tiw.projects.controllers;

import java.io.IOException;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.polimi.tiw.projects.beans.User;
import it.polimi.tiw.projects.dao.UserDAO;
import it.polimi.tiw.projects.exceptions.DAOException;
import it.polimi.tiw.projects.utils.ConnectionHandler;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class SignUp extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private static final Logger logger = LoggerFactory.getLogger(SignUp.class);
	private Connection connection;

	public SignUp() {
		super();
	}

	@Override
	public void init() throws ServletException {
		ServletContext context = getServletContext();
		connection = ConnectionHandler.getConnection(context);
		logger.info("SignUp servlet initialized.");
	}

	@Override
	public void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		logger.debug("Received POST request for user sign up.");
		UserDAO userDAO = new UserDAO(connection);

		String name = req.getParameter("sName").strip();
		String surname = req.getParameter("sSurname").strip();
		String username = req.getParameter("sUsername").strip();
		String password = req.getParameter("sPwd").strip();
		logger.debug("Attempting to sign up user with username: {}", username);

		// Checking that parameters are not empty
		if (name == null || surname == null || username == null || password == null || name.isEmpty()
				|| surname.isEmpty() || password.isEmpty() || username.isEmpty()) {
			logger.warn("Sign up attempt with missing credential values for username: {}", username);
			resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			resp.setContentType("application/json");
			resp.setCharacterEncoding("UTF-8");
			ObjectMapper mapper = new ObjectMapper();
			Map<String, String> errorResponse = new HashMap<>();
			errorResponse.put("error", "Missing credential value");
			resp.getWriter().write(mapper.writeValueAsString(errorResponse));
			return;
		}
		logger.debug("All parameters present for username: {}", username);

		// Try to create a new user
		try {
			userDAO.createUser(username, password, name, surname);
			logger.info("Successfully created user with username: {}", username);
		} catch (DAOException e) {
			logger.warn("DAOException during user creation for username: {}. ErrorType: {}", username, e.getErrorType(),
					e);
			switch (e.getErrorType()) {

			case NAME_ALREADY_EXISTS: {
				logger.warn("Username '{}' already exists.", username);
				resp.setStatus(HttpServletResponse.SC_CONFLICT);
				resp.setContentType("application/json");
				resp.setCharacterEncoding("UTF-8");
				ObjectMapper mapper = new ObjectMapper();
				Map<String, String> errorResponse = new HashMap<>();
				errorResponse.put("error", "Username already taken");
				resp.getWriter().write(mapper.writeValueAsString(errorResponse));
			}
				break;

			default: {
				logger.error("Unhandled DAOException for username: {}. ErrorType: {}. Details: {}", username,
						e.getErrorType(), e.getMessage(), e);
				resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
				resp.setContentType("application/json");
				resp.setCharacterEncoding("UTF-8");
				ObjectMapper mapper = new ObjectMapper();
				Map<String, String> errorResponse = new HashMap<>();
				errorResponse.put("error", "Not possible to sign up");
				resp.getWriter().write(mapper.writeValueAsString(errorResponse));
			}
				break;
			}
			return;
		}

		// If we are here the user was created successfully, let's retrieve it
		User user = null;
		try {
			user = userDAO.checkCredentials(username, password);
			if (user == null) { // Should not happen if createUser was successful and checkCredentials is
								// correct
				logger.error("Failed to retrieve newly created user '{}' - checkCredentials returned null.", username);
				throw new DAOException("Newly created user could not be retrieved.",
						DAOException.DAOErrorType.GENERIC_ERROR);
			}
			logger.debug("Successfully retrieved newly created user: {}", username);
		} catch (DAOException e) {
			logger.error("DAOException while retrieving newly created user '{}'. ErrorType: {}", username,
					e.getErrorType(), e);
			resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			resp.setContentType("application/json");
			resp.setCharacterEncoding("UTF-8");
			ObjectMapper mapper = new ObjectMapper();
			Map<String, String> errorResponse = new HashMap<>();
			errorResponse.put("error", "Failed to retrieve newly created user");
			resp.getWriter().write(mapper.writeValueAsString(errorResponse));
			return;
		}

		// Assign the user to the session
		req.getSession().setAttribute("user", user);
		logger.debug("User {} (ID: {}) set in session after sign up.", user.getUsername(), user.getIdUser());

		// Send user details as JSON
		resp.setStatus(HttpServletResponse.SC_CREATED);
		resp.setContentType("application/json");
		resp.setCharacterEncoding("UTF-8");
		ObjectMapper mapper = new ObjectMapper();
		resp.getWriter().write(mapper.writeValueAsString(user));
		logger.debug("Successfully sent CREATED response with user details for user: {}", user.getUsername());
	}

	@Override
	public void destroy() {
		try {
			ConnectionHandler.closeConnection(connection);
			logger.info("SignUp servlet destroyed. Connection closed.");
		} catch (SQLException e) {
			logger.error("Failed to close database connection on servlet destroy.", e);
		}
	}
}
