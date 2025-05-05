package it.polimi.tiw.projects.controllers;

import java.io.IOException;
import java.sql.*;

import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.WebContext;
import it.polimi.tiw.projects.beans.User;
import it.polimi.tiw.projects.dao.UserDAO;
import it.polimi.tiw.projects.exceptions.DAOException;
import it.polimi.tiw.projects.utils.ConnectionHandler;
import it.polimi.tiw.projects.utils.TemplateHandler;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class SignUp extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private Connection connection;
	private TemplateEngine templateEngine;

	public SignUp() {
		super();
	}

	@Override
	public void init() throws ServletException {
		ServletContext context = getServletContext();
		connection = ConnectionHandler.getConnection(context);
		templateEngine = TemplateHandler.initializeEngine(context);

	}

	@Override
	public void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		UserDAO userDAO = new UserDAO(connection);

		String name = req.getParameter("sName").strip();
		String surname = req.getParameter("sSurname").strip();
		String username = req.getParameter("sUsername").strip();
		String password = req.getParameter("sPwd").strip();

		// Checking that parameters are not empty
		if (name == null || surname == null || username == null || password == null || name.isEmpty()
				|| surname.isEmpty() || password.isEmpty() || username.isEmpty()) {
			resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing credential value");
			return;
		}

		// Try to create a new user
		try {
			userDAO.createUser(username, password, name, surname);
		} catch (DAOException e) {
			switch (e.getErrorType()) {

				case NAME_ALREADY_EXISTS: { // If a user with that name already exists:
					WebContext ctx = TemplateHandler.getWebContext(req, resp, getServletContext());

					ctx.setVariable("errorSignUpMsg", "Username already taken");
					String path = "/index.html";
					templateEngine.process(path, ctx, resp.getWriter());
				}
					break;

				default: { // If another exception occurs
					resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Not possible to sign up");
				}

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
