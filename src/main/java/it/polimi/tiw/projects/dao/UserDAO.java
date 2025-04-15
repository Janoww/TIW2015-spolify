package it.polimi.tiw.projects.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

import it.polimi.tiw.projects.beans.User;
import it.polimi.tiw.projects.exceptions.DAOException;

/**
 * Data Access Object for managing User data in the database.
 * Provides methods for creating, retrieving, and modifying user information.
 */
public class UserDAO {
	private Connection connection;

	/**
	 * Constructs a UserDAO with the given database connection.
	 *
	 * @param connection the database connection to use for DAO operations.
	 */
	public UserDAO(Connection connection) {
		this.connection = connection;
	}

	/**
	 * Creates a new user in the database.
	 * Checks if the username already exists before insertion.
	 *
	 * @param username the username for the new user.
	 * @param pwd      the password for the new user.
	 * @param name     the first name of the new user.
	 * @param surname  the last name of the new user.
	 * @throws DAOException if a database access error occurs or the username
	 *                      already exists.
	 */
	public void createUser(String username, String pwd, String name, String surname) throws DAOException {
		String insertQuery = "INSERT INTO User (idUser, username, password, name, surname) VALUES (UUID_TO_BIN(?), ?, ?, ?, ?)";
		String checkExistence = "SELECT * FROM User WHERE username = ?";
		ResultSet result = null; // Declare outside try-with-resources for the finally block
		try (PreparedStatement checkStatement = connection.prepareStatement(checkExistence);
				PreparedStatement insertStatement = connection.prepareStatement(insertQuery);) {
			checkStatement.setString(1, username);
			result = checkStatement.executeQuery();

			if (result.next()) {
				throw new DAOException("Username already exists", DAOException.DAOErrorType.NAME_ALREADY_EXISTS);
			} else {
				UUID userId = UUID.randomUUID();
				insertStatement.setString(1, userId.toString());
				insertStatement.setString(2, username);
				insertStatement.setString(3, pwd);
				insertStatement.setString(4, name);
				insertStatement.setString(5, surname);
				int affectedRows = insertStatement.executeUpdate();
				if (affectedRows == 0) {
					// Should not happen with a valid insert unless there's a concurrent issue or DB
					// problem
					throw new DAOException("Creating user failed, no rows affected.",
							DAOException.DAOErrorType.GENERIC_ERROR);
				}
			}
		} catch (SQLException e) {
			// Check for unique constraint violation on username (MySQL error code 1062,
			// SQLState '23000')
			// This is a secondary check in case the initial SELECT misses a concurrent
			// insert
			if ("23000".equals(e.getSQLState())) {
				throw new DAOException("Username '" + username + "' already exists.", e,
						DAOException.DAOErrorType.NAME_ALREADY_EXISTS);
			} else {
				throw new DAOException("Error creating user: " + e.getMessage(), e,
						DAOException.DAOErrorType.GENERIC_ERROR);
			}
		} finally {
			try {
				if (result != null)
					result.close();
			} catch (SQLException e) {
				System.err.println("Failed to close ResultSet: " + e.getMessage());
				throw new DAOException("Failed to close resources during user creation", e,
						DAOException.DAOErrorType.GENERIC_ERROR);
			}
		}
	}

	/**
	 * Checks if the provided username and password match a user in the database.
	 *
	 * @param username the username to check.
	 * @param pwd      the password to check.
	 * @return a User object containing the user's details if credentials are valid.
	 * @throws DAOException if a database access error occurs or credentials are
	 *                      invalid.
	 */
	public User checkCredentials(String username, String pwd) throws DAOException {
		String query = "SELECT BIN_TO_UUID(idUser) as idUser, username, name, surname FROM User WHERE username = ? AND password = ?";
		ResultSet result = null; // Declare outside try-with-resources for the finally block
		try (PreparedStatement pStatement = connection.prepareStatement(query);) {
			pStatement.setString(1, username);
			pStatement.setString(2, pwd);
			result = pStatement.executeQuery();
			if (result.next()) { // A user was matched with the password
				User user = new User();
				String userIdStr = result.getString("idUser");
				user.setIdUser(UUID.fromString(userIdStr));
				user.setUsername(result.getString("username"));
				user.setName(result.getString("name"));
				user.setSurname(result.getString("surname"));
				return user;
			} else {
				// No user found with that username/password combination
				throw new DAOException("Invalid credentials", DAOException.DAOErrorType.INVALID_CREDENTIALS);
			}
		} catch (SQLException e) {
			throw new DAOException("Error checking credentials: " + e.getMessage(), e,
					DAOException.DAOErrorType.GENERIC_ERROR);
		} finally {
			try {
				if (result != null)
					result.close();
			} catch (SQLException e) {
				System.err.println("Failed to close ResultSet: " + e.getMessage());
				throw new DAOException("Failed to close resources during credential check", e,
						DAOException.DAOErrorType.GENERIC_ERROR);
			}
		}
	}

	/**
	 * Modifies the name and/or surname of an existing user.
	 * If name or surname parameters are null, the existing values are kept.
	 *
	 * @param user    the User object representing the user to modify (must contain
	 *                the user ID).
	 * @param name    the new first name (or null to keep the existing one).
	 * @param surname the new last name (or null to keep the existing one).
	 * @throws DAOException if a database access error occurs or the user is not
	 *                      found.
	 */
	public void modifyUser(User user, String name, String surname) throws DAOException {
		String query = "UPDATE User SET name = ?, surname = ? WHERE idUser = UUID_TO_BIN(?)";

		String finalName = (name == null) ? user.getName() : name;
		String finalSurname = (surname == null) ? user.getSurname() : surname;

		try (PreparedStatement pStatement = connection.prepareStatement(query);) {
			pStatement.setString(1, finalName);
			pStatement.setString(2, finalSurname);
			pStatement.setString(3, user.getIdUser().toString());
			int affectedRows = pStatement.executeUpdate();

			if (affectedRows == 0) {
				throw new DAOException("User with ID " + user.getIdUser() + " not found for modification.",
						DAOException.DAOErrorType.NOT_FOUND);
			}
			// Update the user bean in memory if modification was successful
			user.setName(finalName);
			user.setSurname(finalSurname);

		} catch (SQLException e) {
			throw new DAOException("Error modifying user: " + e.getMessage(), e,
					DAOException.DAOErrorType.GENERIC_ERROR);
		}
	}
}
