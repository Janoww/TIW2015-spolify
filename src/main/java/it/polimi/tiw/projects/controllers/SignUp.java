package it.polimi.tiw.projects.controllers;

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
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.UnavailableException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class SignUp extends HttpServlet{
	private static final long serialVersionUID = 1L;
	private Connection connection;
	private TemplateEngine templateEngine;
	
	public SignUp() {
		super();
	}
	
	public void init()  throws ServletException {
		try {
			ServletContext context = getServletContext();
			String user = context.getInitParameter("dbUser");
			String password = context.getInitParameter("dbPassword");
			String driver = context.getInitParameter("dbDriver");
			String url = context.getInitParameter("dbUrl");
			
			Class.forName(driver);

			connection = DriverManager.getConnection(url, user, password);
			
			//In Thymeleaf 3.1+, they introduced a new, flexible abstraction layer for web environments called WebApplication, It wraps the standard ServletContext in a higher-level abstraction that Thymeleaf understands.
			JakartaServletWebApplication webApplication = JakartaServletWebApplication.buildApplication(context);
			// We pass the webApplication to new WebApplicationTemplateResolver(webApplication) so the template resolver knows how to find and load HTML templates from your deployed web app.
			WebApplicationTemplateResolver templateResolver = new WebApplicationTemplateResolver(webApplication);
		    // HTML is the default mode, but we will set it anyway for better understanding of code
		    templateResolver.setTemplateMode(TemplateMode.HTML);
		    // This will convert "home" to "home.html"
		    templateResolver.setSuffix(".html");

		    this.templateEngine = new TemplateEngine();
		    templateEngine.setTemplateResolver(templateResolver);
			
			
		} catch (ClassNotFoundException e) {
			throw new UnavailableException("Can't load database driver");
		} catch (SQLException e) {
			throw new UnavailableException("Couldn't get db connection");
		}
		
	}
	
	public void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		UserDAO userDAO = new UserDAO(connection);
		
		String name = req.getParameter("sName");
		String surname = req.getParameter("sSurname");
		String username = req.getParameter("sUsername");
		String password = req.getParameter("sPwd");
			
		if(name == null || surname == null || username == null || password == null || name.isEmpty() || surname.isEmpty() || password.isEmpty() || username.isEmpty()) {
			resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing credential value");
			return;
		}
		
		try {
			userDAO.createUser(username, password, name, surname);
		} catch (DAOException e) {
			switch(e.getErrorType()) {
				case NAME_ALREADY_EXISTS:{
					JakartaServletWebApplication webApplication = JakartaServletWebApplication.buildApplication(getServletContext());
					
					//Contexts should contain all the data required for an execution of the template engine in a variables map, and also reference the locale that must be used for externalized messages.
			        WebContext ctx = new WebContext(webApplication.buildExchange(req, resp), req.getLocale());
					
					ctx.setVariable("errorSignUpMsg", "Username already taken");
					String path = "/index.html";
					templateEngine.process(path, ctx, resp.getWriter());
				}
				break;
				default:{
					resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Not possible to sign up");
				}
			
			}
			return;
		}
		
		User user = null;
		try {
			user = userDAO.checkCredentials(username, password);
		
		} catch (DAOException e) {
			e.printStackTrace();
			resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Not Possible to check credentials");
			return;
		}
		
	
		req.getSession().setAttribute("user", user);
		String path = getServletContext().getContextPath() + "/Home";
		resp.sendRedirect(path);
	}
	
	public void destroy() {
		try {
			if(connection!=null) {
				connection.close();
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
}














