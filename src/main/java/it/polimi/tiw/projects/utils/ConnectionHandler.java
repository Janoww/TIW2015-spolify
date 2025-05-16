package it.polimi.tiw.projects.utils;

import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.servlet.ServletContext;
import jakarta.servlet.UnavailableException;

public class ConnectionHandler {

	private static final Logger logger = LoggerFactory.getLogger(ConnectionHandler.class);

	public static Connection getConnection(ServletContext context) throws UnavailableException {
		DataSource dataSource = (DataSource) context.getAttribute("dataSource");
		if (dataSource == null) {
			logger.error(
					"DataSource not found in ServletContext. Check AppContextListener configuration.");
			throw new UnavailableException(
					"DataSource not initialized. Check AppContextListener configuration.");
		}

		Connection connection = null;
		try {
			logger.debug("Attempting to get connection from DataSource.");
			connection = dataSource.getConnection();
			logger.debug("Successfully obtained connection from DataSource.");
		} catch (SQLException e) {
			logger.error("Error getting connection from DataSource pool.", e);
			throw new UnavailableException(
					"Couldn't get DB connection from pool: " + e.getMessage());
		}
		return connection;
	}

	/**
	 * Closes a connection, returning it to the pool.
	 *
	 * @param connection the connection to close.
	 * @throws SQLException if a database access error occurs.
	 */
	public static void closeConnection(Connection connection) throws SQLException {
		if (connection != null) {
			try {
				logger.debug("Closing connection (returning to pool).");
				connection.close();
				logger.debug("Connection closed successfully.");
			} catch (SQLException e) {
				logger.error("Error closing connection (returning to pool).", e);
				throw e;
			}
		}
	}
}
