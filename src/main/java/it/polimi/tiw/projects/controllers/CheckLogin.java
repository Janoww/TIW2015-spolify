package it.polimi.tiw.projects.controllers;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.sql.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.WebContext;
import it.polimi.tiw.projects.beans.User;
import it.polimi.tiw.projects.dao.UserDAO;
import it.polimi.tiw.projects.exceptions.DAOException;
import it.polimi.tiw.projects.utils.ConnectionHandler;
import it.polimi.tiw.projects.utils.TemplateHandler;

public class CheckLogin extends HttpServlet {
	private static final Logger logger = LoggerFactory.getLogger(CheckLogin.class);
	private static final long serialVersionUID = 1L;
	private Connection connection = null;
	private TemplateEngine templateEngine;

	public CheckLogin() {
		super();
	}

	@Override
	public void init() throws ServletException {
		ServletContext context = getServletContext();
		connection = ConnectionHandler.getConnection(context);
		templateEngine = TemplateHandler.initializeEngine(context);
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		UserDAO userDAO = new UserDAO(connection);
		User user = null;

		String username = req.getParameter("lUsername").strip();
		String password = req.getParameter("lPwd").strip();
		

		// Checking if the parameters are empty
		if (username == null || password == null || username.isEmpty() || password.isEmpty()) {
			logger.warn("Missing credential value");
			resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing credential value");
			return;
		}

		// Searching for the user
		try {
			user = userDAO.checkCredentials(username, password);
		} catch (DAOException e) {
			switch (e.getErrorType()) {
				case INVALID_CREDENTIALS: { // No user found with that username/password combination
					logger.warn("Invelid credentials");
					WebContext ctx = TemplateHandler.getWebContext(req, resp, getServletContext());

					ctx.setVariable("errorLogInMsg",
							"No user found with that username/password combination");
					String path = "/index.html";
					templateEngine.process(path, ctx, resp.getWriter());
					return;
				}
				default: { // If another exception occurs
					resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
							"Not possible to log in");
					return;
				}

			}
		}
		
		logger.info("User {} logged in", user.getUsername());

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
