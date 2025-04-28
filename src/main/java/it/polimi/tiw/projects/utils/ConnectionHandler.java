package it.polimi.tiw.projects.utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import jakarta.servlet.ServletContext;
import jakarta.servlet.UnavailableException;

public class ConnectionHandler {
	public static Connection getConnection(ServletContext context) throws UnavailableException {
		Connection connection = null;
		try {
			String driver = context.getInitParameter("dbDriver");
			String user = context.getInitParameter("dbUser");
			String password = context.getInitParameter("dbPassword");
			String url = context.getInitParameter("dbUrl");
			Class.forName(driver);

			connection = DriverManager.getConnection(url, user, password);
		} catch (ClassNotFoundException e) {
			throw new UnavailableException("Can't load database driver");
		} catch (SQLException e) {
			throw new UnavailableException("Couldn't get db connection");
		}
		return connection;
	}

	public static void closeConnection(Connection connection) throws SQLException {
		if (connection != null) {
			connection.close();
		}
	}
}
