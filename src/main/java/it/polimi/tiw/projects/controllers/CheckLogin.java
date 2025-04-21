package it.polimi.tiw.projects.controllers;

import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.UnavailableException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.*;

import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.WebApplicationTemplateResolver;
import org.thymeleaf.web.servlet.JakartaServletWebApplication;

import it.polimi.tiw.projects.beans.User;
import it.polimi.tiw.projects.dao.UserDAO;
import it.polimi.tiw.projects.exceptions.DAOException;

public class CheckLogin extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private Connection connection = null;
	private TemplateEngine templateEngine;

	@Override
	public void init() throws ServletException {
		try {
			ServletContext context = getServletContext();
			String driver = context.getInitParameter("dbDriver");
			String url = context.getInitParameter("dbUrl");
			String user = context.getInitParameter("dbUser");
			String password = context.getInitParameter("dbPassword");

			Class.forName(driver); // In previous versions of JDBC, to obtain a connection, you first had to
									// initialize your JDBC driver by calling the method Class.forName

			connection = DriverManager.getConnection(url, user, password);

			JakartaServletWebApplication webApplication = JakartaServletWebApplication.buildApplication(context);

			WebApplicationTemplateResolver templateResolver = new WebApplicationTemplateResolver(webApplication);

			templateResolver.setTemplateMode(TemplateMode.HTML);
			templateResolver.setSuffix(".html");

			this.templateEngine = new TemplateEngine();

			templateEngine.setTemplateResolver(templateResolver);

		} catch (SQLException e) {
			throw new UnavailableException("Couldn't get db connection");
		} catch (ClassNotFoundException e) {
			throw new UnavailableException("Can't load database driver");
		}

	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		UserDAO userDAO = new UserDAO(connection);
		User user = null;

		String username = req.getParameter("lUsername");
		String password = req.getParameter("lPwd");

		if (username == null || password == null || username.isEmpty() || password.isEmpty()) {
			resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing credential value");
			return;
		}

		try {
			user = userDAO.checkCredentials(username, password);
		} catch (DAOException e) {
			switch (e.getErrorType()) {
			case INVALID_CREDENTIALS: {
				JakartaServletWebApplication webApplication = JakartaServletWebApplication
						.buildApplication(getServletContext());
				WebContext ctx = new WebContext(webApplication.buildExchange(req, resp), req.getLocale());

				ctx.setVariable("errorLogInMsg", "No user found with that username/password combination");
				String path = "/index.html";
				templateEngine.process(path, ctx, resp.getWriter());
			}
				break;

			}
			return;

		}

		req.getSession().setAttribute("user", user);
		String path = getServletContext().getContextPath() + "/Home";
		resp.sendRedirect(path);

	}

	public void destroy() {
		try {
			if (connection != null) {
				connection.close();
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

}