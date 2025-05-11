package it.polimi.tiw.projects.controllers;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

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

public class CheckLogin extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private static final Logger logger = LoggerFactory.getLogger(CheckLogin.class);
	private Connection connection = null;

	public CheckLogin() {
		super();
	}

	@Override
	public void init() throws ServletException {
		ServletContext context = getServletContext();
		connection = ConnectionHandler.getConnection(context);
		logger.info("CheckLogin servlet initialized.");
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		logger.debug("Received POST request for user login.");
		UserDAO userDAO = new UserDAO(connection);
		User user = null;

		String username = req.getParameter("lUsername").strip();
		String password = req.getParameter("lPwd").strip();
		logger.debug("Attempting login for username: {}", username);

		// Checking if the parameters are empty
		if (username == null || password == null || username.isEmpty() || password.isEmpty()) {
			logger.warn("Login attempt with missing credentials for username: {}", username);
			resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			resp.setContentType("application/json");
			resp.setCharacterEncoding("UTF-8");
			ObjectMapper mapper = new ObjectMapper();
			Map<String, String> errorResponse = new HashMap<>();
			errorResponse.put("error", "Missing credential value");
			resp.getWriter().write(mapper.writeValueAsString(errorResponse));
			return;
		}

		// Searching for the user
		try {
			user = userDAO.checkCredentials(username, password);
			logger.info("User {} successfully authenticated.", username);
		} catch (DAOException e) {
			logger.warn("DAOException during login for username: {}. ErrorType: {}", username, e.getErrorType(), e);
			switch (e.getErrorType()) {
			case INVALID_CREDENTIALS: { // No user found with that username/password combination
				logger.warn("Invalid credentials for username: {}", username);
				resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
				resp.setContentType("application/json");
				resp.setCharacterEncoding("UTF-8");
				ObjectMapper mapper = new ObjectMapper();
				Map<String, String> errorResponse = new HashMap<>();
				errorResponse.put("error", "Invalid username or password");
				resp.getWriter().write(mapper.writeValueAsString(errorResponse));
				return;
			}
			default: { // If another exception occurs
				logger.error("Unhandled DAOException for username: {}. ErrorType: {}. Details: {}", username,
						e.getErrorType(), e.getMessage(), e);
				resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
				resp.setContentType("application/json");
				resp.setCharacterEncoding("UTF-8");
				ObjectMapper mapper = new ObjectMapper();
				Map<String, String> errorResponse = new HashMap<>();
				errorResponse.put("error", "Not possible to log in");
				resp.getWriter().write(mapper.writeValueAsString(errorResponse));
				return;
			}

			}
		}

		// Assign the user to the session
		req.getSession().setAttribute("user", user);
		logger.debug("User {} (ID: {}) set in session.", user.getUsername(), user.getIdUser());

		// Send user details as JSON
		resp.setStatus(HttpServletResponse.SC_OK);
		resp.setContentType("application/json");
		resp.setCharacterEncoding("UTF-8");
		ObjectMapper mapper = new ObjectMapper();
		resp.getWriter().write(mapper.writeValueAsString(user));
		logger.debug("Successfully sent OK response with user details for user: {}", user.getUsername());
	}

	@Override
	public void destroy() {
		try {
			ConnectionHandler.closeConnection(connection);
			logger.info("CheckLogin servlet destroyed. Connection closed.");
		} catch (SQLException e) {
			logger.error("Failed to close database connection on servlet destroy.", e);
		}
	}

}
