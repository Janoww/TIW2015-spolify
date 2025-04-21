package it.polimi.tiw.projects.controllers;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServlet;

public class addSongToPL extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private Connection connection;

	public void init() {
		try {
			ServletContext context = getServletContext();
			String user = context.getInitParameter("dbUsr");
			String password = context.getInitParameter("dbPassword");
			String driver = context.getInitParameter("dbDriver");
			String url = context.getInitParameter("dbUrl");

			Class.forName(driver);

			connection = DriverManager.getConnection(url, user, password);
		} catch (ClassNotFoundException | SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public void doPost() {

	}
}
