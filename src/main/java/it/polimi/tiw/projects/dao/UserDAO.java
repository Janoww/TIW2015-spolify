package it.polimi.tiw.projects.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.polimi.tiw.projects.beans.User;
import it.polimi.tiw.projects.exceptions.DAOException;

/**
 * Data Access Object for managing User data in the database. Provides methods
 * for creating, retrieving, and modifying user information.
 */
public class UserDAO {
	private static final Logger logger = LoggerFactory.getLogger(UserDAO.class);
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
	 * Creates a new user in the database. Checks if the username already exists
	 * before insertion.
	 *
	 * @param username the username for the new user.
	 * @param pwd      the password for the new user.
	 * @param name     the first name of the new user.
	 * @param surname  the last name of the new user.
	 * @throws DAOException if the username already exists
	 *                      ({@link it.polimi.tiw.projects.exceptions.DAOException.DAOErrorType#NAME_ALREADY_EXISTS})
	 *                      or another database access error occurs
	 *                      ({@link it.polimi.tiw.projects.exceptions.DAOException.DAOErrorType#GENERIC_ERROR}).
	 * @return The newly created User object with its generated ID.
	 */
	public User createUser(String username, String pwd, String name, String surname) throws DAOException {
		logger.debug("Attempting to create user: username={}, name={}, surname={}", username, name, surname);
		String insertQuery = "INSERT INTO User (idUser, username, password, name, surname) VALUES (UUID_TO_BIN(?), ?, ?, ?, ?)";
		String checkExistence = "SELECT * FROM User WHERE username = ?";
		try (PreparedStatement checkStatement = connection.prepareStatement(checkExistence);
				PreparedStatement insertStatement = connection.prepareStatement(insertQuery);) {
			checkStatement.setString(1, username);
			try (ResultSet result = checkStatement.executeQuery()) {
				if (result.next()) {
					logger.warn("User creation failed: Username '{}' already exists.", username);
					throw new DAOException("Username already exists", DAOException.DAOErrorType.NAME_ALREADY_EXISTS);
				} else {
					UUID userId = UUID.randomUUID();
					logger.debug("Generated new user ID: {}", userId);
					insertStatement.setString(1, userId.toString());
					insertStatement.setString(2, username);
					insertStatement.setString(3, pwd);
					insertStatement.setString(4, name);
					insertStatement.setString(5, surname);
					int affectedRows = insertStatement.executeUpdate();
					if (affectedRows == 0) {
						logger.error("Creating user failed, no rows affected for username: {}", username);
						// Should not happen with a valid insert unless there's a concurrent issue
						// or DB
						// problem
						throw new DAOException("Creating user failed, no rows affected.",
								DAOException.DAOErrorType.GENERIC_ERROR);
					}
					logger.info("User '{}' created successfully with ID: {}", username, userId);

					User newUser = new User();
					newUser.setIdUser(userId);
					newUser.setUsername(username);
					newUser.setName(name);
					newUser.setSurname(surname);
					// Password is not set in the User bean for security
					return newUser;
				}
			}
		} catch (SQLException e) {
			// Check for unique constraint violation on username (SQLState '23000')
			if ("23000".equals(e.getSQLState())) { // NAME_ALREADY_EXISTS
				logger.warn(
						"User creation failed for username {}: Username already exists (caught during INSERT). Details: SQLState={}, Message={}",
						username, e.getSQLState(), e.getMessage());
				throw new DAOException("Username '" + username + "' already exists.", e,
						DAOException.DAOErrorType.NAME_ALREADY_EXISTS);
			} else { // GENERIC_ERROR
				logger.error("SQL error during user creation for username {}: SQLState={}, Message={}", username,
						e.getSQLState(), e.getMessage(), e);
				throw new DAOException("Error creating user: " + e.getMessage(), e,
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
	 * @throws DAOException if the credentials are invalid
	 *                      ({@link it.polimi.tiw.projects.exceptions.DAOException.DAOErrorType#INVALID_CREDENTIALS})
	 *                      or another database access error occurs
	 *                      ({@link it.polimi.tiw.projects.exceptions.DAOException.DAOErrorType#GENERIC_ERROR}).
	 */
	public User checkCredentials(String username, String pwd) throws DAOException {
		logger.debug("Attempting to check credentials for username: {}", username);
		String query = "SELECT BIN_TO_UUID(idUser) as idUser, username, name, surname FROM User WHERE username = ? AND password = ?";
		try (PreparedStatement pStatement = connection.prepareStatement(query);) {
			pStatement.setString(1, username);
			pStatement.setString(2, pwd);
			try (ResultSet result = pStatement.executeQuery()) {
				if (result.next()) { // A user was matched with the password
					User user = new User();
					String userIdStr = result.getString("idUser");
					user.setIdUser(UUID.fromString(userIdStr));
					user.setUsername(result.getString("username"));
					user.setName(result.getString("name"));
					user.setSurname(result.getString("surname"));
					logger.info("Credentials valid for username: {}", username);
					return user;
				} else {
					logger.warn("Invalid credentials provided for username: {}", username);
					// No user found with that username/password combination
					throw new DAOException("Invalid credentials", DAOException.DAOErrorType.INVALID_CREDENTIALS);
				}
			}
		} catch (SQLException e) { // GENERIC_ERROR
			logger.error("SQL error checking credentials for username {}: {}", username, e.getMessage(), e);
			throw new DAOException("Error checking credentials: " + e.getMessage(), e,
					DAOException.DAOErrorType.GENERIC_ERROR);
		} catch (IllegalArgumentException e) { // GENERIC_ERROR (expected for bad UUID data)
			logger.error("Error parsing UUID for user {} during credential check: {}", username, e.getMessage());
			throw new DAOException("Error processing user data during credential check: " + e.getMessage(), e,
					DAOException.DAOErrorType.GENERIC_ERROR);
		}
	}

	/**
	 * Modifies the name and/or surname of an existing user. If name or surname
	 * parameters are null, the existing values are kept.
	 *
	 * @param user    the User object representing the user to modify (must contain
	 *                the user ID).
	 * @param name    the new first name (or null to keep the existing one).
	 * @param surname the new last name (or null to keep the existing one).
	 * @throws DAOException if the user is not found
	 *                      ({@link it.polimi.tiw.projects.exceptions.DAOException.DAOErrorType#NOT_FOUND})
	 *                      or another database access error occurs
	 *                      ({@link it.polimi.tiw.projects.exceptions.DAOException.DAOErrorType#GENERIC_ERROR}).
	 */
	public void modifyUser(User user, String name, String surname) throws DAOException {
		logger.debug("Attempting to modify user ID: {} with name={}, surname={}", user.getIdUser(), name, surname);
		String query = "UPDATE User SET name = ?, surname = ? WHERE idUser = UUID_TO_BIN(?)";

		String finalName = (name == null) ? user.getName() : name;
		String finalSurname = (surname == null) ? user.getSurname() : surname;

		try (PreparedStatement pStatement = connection.prepareStatement(query);) {
			pStatement.setString(1, finalName);
			pStatement.setString(2, finalSurname);
			pStatement.setString(3, user.getIdUser().toString());
			int affectedRows = pStatement.executeUpdate();

			if (affectedRows == 0) {
				logger.warn("User modification failed: User ID {} not found.", user.getIdUser());
				throw new DAOException("User with ID " + user.getIdUser() + " not found for modification.",
						DAOException.DAOErrorType.NOT_FOUND);
			}
			// Update the user bean in memory if modification was successful
			logger.info("User ID {} modified successfully. New name={}, surname={}", user.getIdUser(), finalName,
					finalSurname);
			user.setName(finalName);
			user.setSurname(finalSurname);

		} catch (SQLException e) {
			logger.error("SQL error modifying user ID {}: {}", user.getIdUser(), e.getMessage(), e);
			throw new DAOException("Error modifying user: " + e.getMessage(), e,
					DAOException.DAOErrorType.GENERIC_ERROR);
		}
	}
}
