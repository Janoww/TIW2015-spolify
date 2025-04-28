package it.polimi.tiw.projects.controllers;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import it.polimi.tiw.projects.utils.ConnectionHandler;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;

public class AddSongToPL extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private Connection connection;
	
	public AddSongToPL() {
		super();
	}
	
	@Override
	public void init() throws ServletException{
		ServletContext context = getServletContext();
		connection = ConnectionHandler.getConnection(context);

	}

	public void doPost() {

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
