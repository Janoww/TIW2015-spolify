package it.polimi.tiw.projects.controllers;

import java.sql.Connection;
import java.sql.SQLException;

import it.polimi.tiw.projects.utils.ConnectionHandler;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class AddSongToPL extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private Connection connection;

	public AddSongToPL() {
		super();
	}

	@Override
	public void init() throws ServletException {
		ServletContext context = getServletContext();
		connection = ConnectionHandler.getConnection(context);
	}
	
	@Override
	public void doPost(HttpServletRequest req, HttpServletResponse resp) {
		

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
