package it.polimi.tiw.projects.controllers;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import org.thymeleaf.TemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.WebApplicationTemplateResolver;
import org.thymeleaf.web.servlet.JakartaServletWebApplication;

import it.polimi.tiw.projects.beans.User;
import it.polimi.tiw.projects.dao.PlaylistDAO;
import it.polimi.tiw.projects.exceptions.DAOException;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.UnavailableException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class GoToHome extends HttpServlet {
 static final long serialVersionUID = 1L;
	private Connection connection;
	private TemplateEngine templateEngine;
	
	public void init() throws ServletException {
		
		try {
		ServletContext context = getServletContext();
		String driver = context.getInitParameter("dbDriver");
		String user = context.getInitParameter("dbUser");
		String password = context.getInitParameter("dbPassword");
		String url = context.getInitParameter("dbUrl");
		Class.forName(driver);
		
		connection = DriverManager.getConnection(url, user, password);
		
		JakartaServletWebApplication webApplication = JakartaServletWebApplication.buildApplication(context);
		
		WebApplicationTemplateResolver templateResolver = new WebApplicationTemplateResolver(webApplication);
		
		templateResolver.setTemplateMode(TemplateMode.HTML);
		
		templateResolver.setSuffix(".html");
		
		templateEngine = new TemplateEngine();
		templateEngine.setTemplateResolver(templateResolver);
		
		} catch (SQLException e) {
			e.printStackTrace();
			throw new UnavailableException("Couldn't get db connection");
		} catch (ClassNotFoundException e) {
			throw new UnavailableException("Can't load database driver");
		}
	}
	
	public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException{
		PlaylistDAO playlistDAO = new PlaylistDAO(connection);
		
		UUID userId = ((User) req.getSession().getAttribute("user")).getIdUser();
		
		try {
			List<Integer> playlistIDs = playlistDAO.findPlaylistsByUser(userId);
		} catch (DAOException e) {
			e.printStackTrace();
			resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Not possible to sign up");
		}
		
		
		
		
		
		
		
		

	}
}
