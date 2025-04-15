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
	 * @throws SQLException if a database access error occurs.
	 * @throws DAOException if the username already exists.
	 */
	public void createUser(String username, String pwd, String name, String surname) throws SQLException, DAOException {
		String insertQuery = "INSERT INTO User (idUser, username, password, name, surname) VALUES (UUID_TO_BIN(?), ?, ?, ?, ?)";
		String checkExistence = "SELECT * FROM User WHERE username = ?";
		try (PreparedStatement checkStatement = connection.prepareStatement(checkExistence);
				PreparedStatement insertStatement = connection.prepareStatement(insertQuery);) {
			checkStatement.setString(1, username);
			ResultSet result = checkStatement.executeQuery();

			if (result.next())
				throw new DAOException("Username already exists", DAOException.DAOErrorType.USERNAME_ALREADY_EXISTS);
			else {
				UUID userId = UUID.randomUUID();
				insertStatement.setString(1, userId.toString());
				insertStatement.setString(2, username);
				insertStatement.setString(3, pwd);
				insertStatement.setString(4, name);
				insertStatement.setString(5, surname);
				insertStatement.executeUpdate();
			}
		}
	}

	/**
	 * Checks if the provided username and password match a user in the database.
	 *
	 * @param username the username to check.
	 * @param pwd      the password to check.
	 * @return a User object containing the user's details if credentials are valid.
	 * @throws SQLException if a database access error occurs.
	 * @throws DAOException if the credentials are invalid (no matching user found).
	 */
	public User checkCredentials(String username, String pwd) throws SQLException, DAOException {
		String query = "SELECT BIN_TO_UUID(idUser) as idUser, username, name, surname FROM User WHERE username = ? AND password = ?";
		try (PreparedStatement pStatement = connection.prepareStatement(query);) {
			pStatement.setString(1, username);
			pStatement.setString(2, pwd);
			try (ResultSet result = pStatement.executeQuery();) {
				if (result.next()) { // A user was matched with the password
					User user = new User();
					String userIdStr = result.getString("idUser");
					user.setIdUser(UUID.fromString(userIdStr));
					user.setUsername(result.getString("username"));
					user.setName(result.getString("name"));
					user.setSurname(result.getString("surname"));
					return user;
				} else
					throw new DAOException("Invalid credentials", DAOException.DAOErrorType.INVALID_CREDENTIALS);

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
	 * @throws SQLException if a database access error occurs.
	 */
	public void modifyUser(User user, String name, String surname) throws SQLException {
		String query = "UPDATE User SET name = ?, surname = ? WHERE idUser = UUID_TO_BIN(?)";

		if (name == null)
			name = user.getName();
		if (surname == null)
			surname = user.getSurname();

		try (PreparedStatement pStatement = connection.prepareStatement(query);) {
			pStatement.setString(1, name);
			pStatement.setString(2, surname);
			pStatement.setString(3, user.getIdUser().toString());
			pStatement.executeUpdate();
		}
	}
}
