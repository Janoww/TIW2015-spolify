package it.polimi.tiw.projects.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import it.polimi.tiw.projects.beans.User;
import it.polimi.tiw.projects.exceptions.DAOException;

public class UserDAO {
	private Connection connection;

	public UserDAO(Connection connection) {
		this.connection = connection;
	}

	public void createUser(String username, String pwd, String name, String surname) throws SQLException, DAOException {
		String insertQuery = "INSERT INTO User (username, password, name, surname) VALUES (?, ?, ?, ?)"; // Corrected
																											// table
																											// name case
		String checkExistence = "SELECT * FROM User WHERE username = ?"; // Corrected table name case
		try (PreparedStatement checkStatement = connection.prepareStatement(checkExistence);
				PreparedStatement insertStatement = connection.prepareStatement(insertQuery);) {
			checkStatement.setString(1, username);
			ResultSet result = checkStatement.executeQuery();

			if (result.next())
				throw new DAOException("Username already exists", DAOException.DAOErrorType.USERNAME_ALREADY_EXISTS);
			else {
				insertStatement.setString(1, username);
				insertStatement.setString(2, pwd);
				insertStatement.setString(3, name);
				insertStatement.setString(4, surname);
				insertStatement.executeUpdate();
			}
		}
	}

	public User checkCredentials(String username, String pwd) throws SQLException, DAOException {
		String query = "SELECT  idUser, username, name, surname FROM User  WHERE username = ? AND password =?"; // Corrected
																												// table
																												// name
																												// case
		try (PreparedStatement pStatement = connection.prepareStatement(query);) { // Sanitized
			pStatement.setString(1, username);
			pStatement.setString(2, pwd);
			try (ResultSet result = pStatement.executeQuery();) {
				if (result.next()) { // A user was matched with the password
					User user = new User();
					user.setIdUser(result.getInt("idUser"));
					user.setUsername(result.getString("username"));
					user.setName(result.getString("name"));
					user.setSurname(result.getString("surname"));
					return user;
				} else
					throw new DAOException("Invalid credentials", DAOException.DAOErrorType.INVALID_CREDENTIALS);

			}
		}
	}

	public void modifyUser(User user, String name, String surname) throws SQLException, DAOException {
		String query = "UPDATE User SET name = ?, surname = ? WHERE idUser = ?"; // Corrected table name case

		if (name == null)
			name = user.getName();
		if (surname == null)
			surname = user.getSurname();

		try (PreparedStatement pStatement = connection.prepareStatement(query);) {
			pStatement.setString(1, name);
			pStatement.setString(2, surname);
			pStatement.setInt(3, user.getIdUser());
			pStatement.executeUpdate();
		}
	}
}
