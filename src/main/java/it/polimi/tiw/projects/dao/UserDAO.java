package it.polimi.tiw.projects.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import it.polimi.tiw.projects.beans.User;

public class UserDAO {
	private Connection connection;

	public UserDAO(Connection connection) {
		this.connection = connection;
	}

	public User checkCredentials(String username, String pwd) throws SQLException {
		String query = "SELECT  idUser, username, name, surname FROM user  WHERE username = ? AND password =?";
		try (PreparedStatement pStatement = connection.prepareStatement(query);) { // Sanitized
			pStatement.setString(1, username);
			pStatement.setString(2, pwd);
			try (ResultSet result = pStatement.executeQuery();) {
				if (!result.isBeforeFirst()) // no results, credential check failed
					return null;
				else {
					result.next();
					User user = new User();
					user.setIdUser(result.getInt("idUser"));
					user.setUsername(result.getString("username"));
					user.setName(result.getString("name"));
					user.setSurname(result.getString("surname"));
					return user;
				}
			}
		}
	}
}
