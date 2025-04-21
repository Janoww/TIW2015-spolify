package it.polimi.tiw.projects.dao;

import it.polimi.tiw.projects.beans.Album;
import it.polimi.tiw.projects.exceptions.DAOException;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class AlbumDAO {
	private Connection connection;

	public AlbumDAO(Connection connection) {
		this.connection = connection;
	}

	/**
	 * Creates a new album in the database.
	 *
	 * @param name   The name of the album.
	 * @param year   The release year of the album.
	 * @param artist The artist of the album.
	 * @param idUser The UUID of the user creating the album.
	 * @return The newly created Album object with its generated ID.
	 * @throws DAOException if a database access error occurs or the name already
	 *                      exists for this user.
	 */
	public Album createAlbum(String name, int year, String artist, UUID idUser) throws DAOException {
		String query = "INSERT into Album (name, year, artist, idUser) VALUES(?, ?, ?, UUID_TO_BIN(?))";
		Album newAlbum = null;
		ResultSet generatedKeys = null;

		try (PreparedStatement pStatement = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
			pStatement.setString(1, name);
			pStatement.setInt(2, year);
			pStatement.setString(3, artist);
			pStatement.setString(4, idUser.toString());
			int affectedRows = pStatement.executeUpdate();

			if (affectedRows == 0) {
				// This case might not happen with auto-increment keys but kept for robustness
				throw new DAOException("Creating album failed, no rows affected.",
						DAOException.DAOErrorType.GENERIC_ERROR);
			}

			generatedKeys = pStatement.getGeneratedKeys();
			if (generatedKeys.next()) {
				int newId = generatedKeys.getInt(1);
				// Create the Album bean
				newAlbum = new Album();
				newAlbum.setIdAlbum(newId);
				newAlbum.setName(name);
				newAlbum.setYear(year);
				newAlbum.setArtist(artist);
				newAlbum.setIdUser(idUser);
			} else {
				throw new DAOException("Creating album failed, no ID obtained.",
						DAOException.DAOErrorType.GENERIC_ERROR);
			}
		} catch (SQLException e) {
			// Check for unique constraint violation (MySQL error code 1062, SQLState
			// '23000') - Handles the composite unique key (name, idUser)
			if ("23000".equals(e.getSQLState())) {
				throw new DAOException("Album name '" + name + "' already exists for this user.", e,
						DAOException.DAOErrorType.NAME_ALREADY_EXISTS);
			} else {
				throw new DAOException("Error creating album: " + e.getMessage(), e,
						DAOException.DAOErrorType.GENERIC_ERROR);
			}
		} finally {
			try {
				if (generatedKeys != null)
					generatedKeys.close();
			} catch (SQLException e) {
				System.err.println("Failed to close ResultSet: " + e.getMessage());
				throw new DAOException("Failed to close resources", e, DAOException.DAOErrorType.GENERIC_ERROR);
			}
		}
		return newAlbum;
	}

	/**
	 * Finds an album by its ID.
	 *
	 * @param idAlbum The ID of the album to find.
	 * @return The Album object if found, null otherwise.
	 * @throws DAOException if a database access error occurs.
	 */
	public Album findAlbumById(int idAlbum) throws DAOException {
		Album album = null;
		String query = "SELECT idAlbum, name, year, artist, BIN_TO_UUID(idUser) as idUser FROM Album WHERE idAlbum = ?";
		try (PreparedStatement pStatement = connection.prepareStatement(query)) {
			pStatement.setInt(1, idAlbum);
			try (ResultSet result = pStatement.executeQuery()) {
				if (result.next()) {
					album = new Album();
					album.setIdAlbum(result.getInt("idAlbum"));
					album.setName(result.getString("name"));
					album.setYear(result.getInt("year"));
					album.setArtist(result.getString("artist"));
					album.setIdUser(UUID.fromString(result.getString("idUser")));
				}
			}
		} catch (SQLException e) {
			throw new DAOException("Error finding album by ID: " + e.getMessage(), e,
					DAOException.DAOErrorType.GENERIC_ERROR);
		}
		return album;
	}

	/**
	 * Finds all albums in the database.
	 *
	 * @return A list of all albums.
	 * @throws DAOException if a database access error occurs.
	 */
	public List<Album> findAllAlbums() throws DAOException {
		List<Album> albums = new ArrayList<>();
		String query = "SELECT idAlbum, name, year, artist, BIN_TO_UUID(idUser) as idUser FROM Album ORDER BY artist, year, name";
		try (Statement statement = connection.createStatement(); ResultSet result = statement.executeQuery(query)) {
			while (result.next()) {
				Album album = new Album();
				album.setIdAlbum(result.getInt("idAlbum"));
				album.setName(result.getString("name"));
				album.setYear(result.getInt("year"));
				album.setArtist(result.getString("artist"));
				album.setIdUser(UUID.fromString(result.getString("idUser")));
				albums.add(album);
			}
		} catch (SQLException e) {
			throw new DAOException("Error finding all albums: " + e.getMessage(), e,
					DAOException.DAOErrorType.GENERIC_ERROR);
		}
		return albums;
	}

	/**
	 * Finds all albums created by a specific user.
	 *
	 * @param userId The UUID of the user.
	 * @return A list of albums created by the user, ordered by year and name.
	 * @throws DAOException if a database access error occurs.
	 */
	public List<Album> findAlbumsByUser(UUID userId) throws DAOException {
		List<Album> userAlbums = new ArrayList<>();
		String query = "SELECT idAlbum, name, year, artist, BIN_TO_UUID(idUser) as idUser FROM Album WHERE idUser = UUID_TO_BIN(?) ORDER BY year, name";
		ResultSet result = null;

		try (PreparedStatement pStatement = connection.prepareStatement(query)) {
			pStatement.setString(1, userId.toString());
			result = pStatement.executeQuery();
			while (result.next()) {
				Album album = new Album();
				album.setIdAlbum(result.getInt("idAlbum"));
				album.setName(result.getString("name"));
				album.setYear(result.getInt("year"));
				album.setArtist(result.getString("artist"));
				album.setIdUser(UUID.fromString(result.getString("idUser")));
				userAlbums.add(album);
			}
		} catch (SQLException e) {
			throw new DAOException("Error finding albums by user: " + e.getMessage(), e,
					DAOException.DAOErrorType.GENERIC_ERROR);
		} finally {
			try {
				if (result != null)
					result.close();
			} catch (SQLException e) {
				System.err.println("Failed to close ResultSet finding albums by user: " + e.getMessage());
				throw new DAOException("Failed to close resources finding albums by user", e,
						DAOException.DAOErrorType.GENERIC_ERROR);
			}
		}
		return userAlbums;
	}

	/**
	 * Updates an existing album in the database, only modifying fields with
	 * non-null values.
	 *
	 * @param idAlbum The ID of the album to update.
	 * @param userId  The UUID of the user attempting the update (for
	 *                authorization).
	 * @param name    The new name for the album (or null to keep existing).
	 * @param year    The new release year for the album (or null to keep existing).
	 * @param artist  The new artist for the album (or null to keep existing).
	 * @return true if the update was successful (at least one field updated), false
	 *         otherwise.
	 * @throws DAOException             if a database access error occurs, the user
	 *                                  is not authorized, or the new name already
	 *                                  exists for this user.
	 * @throws IllegalArgumentException if all update parameters (name, year,
	 *                                  artist) are null.
	 */
	public boolean updateAlbum(int idAlbum, UUID userId, String name, Integer year, String artist) throws DAOException {
		// Build the query dynamically
		StringBuilder queryBuilder = new StringBuilder("UPDATE Album SET ");
		List<Object> params = new ArrayList<>();
		boolean firstField = true;

		if (name != null) {
			queryBuilder.append("name = ?");
			params.add(name);
			firstField = false;
		}
		if (year != null) {
			if (!firstField)
				queryBuilder.append(", ");
			queryBuilder.append("year = ?");
			params.add(year);
			firstField = false;
		}
		if (artist != null) {
			if (!firstField)
				queryBuilder.append(", ");
			queryBuilder.append("artist = ?");
			params.add(artist);
			firstField = false;
		}

		// Check if any field was actually added for update
		if (params.isEmpty()) {
			throw new IllegalArgumentException("No fields provided for update.");
		}

		// Add authorization check
		queryBuilder.append(" WHERE idAlbum = ? AND idUser = UUID_TO_BIN(?)");
		params.add(idAlbum);
		params.add(userId.toString());

		String query = queryBuilder.toString();

		try (PreparedStatement pStatement = connection.prepareStatement(query)) {
			// Set parameters dynamically
			for (int i = 0; i < params.size(); i++) {
				Object param = params.get(i);
				if (param instanceof String) {
					pStatement.setString(i + 1, (String) param);
				} else if (param instanceof Integer) {
					pStatement.setInt(i + 1, (Integer) param);
				}
			}

			int affectedRows = pStatement.executeUpdate();
			if (affectedRows == 0) {
				throw new DAOException("Album with ID " + idAlbum + " not found for update or user not authorized.",
						DAOException.DAOErrorType.NOT_FOUND); // Or introduce UNAUTHORIZED type
			}
			return true; // If we reach here, affectedRows must be > 0
		} catch (SQLException e) {
			// Check for unique constraint violation (name, idUser)
			if ("23000".equals(e.getSQLState()) && name != null) {
				throw new DAOException("Album name '" + name + "' already exists for this user.", e,
						DAOException.DAOErrorType.NAME_ALREADY_EXISTS);
			} else {
				throw new DAOException("Error updating album: " + e.getMessage(), e,
						DAOException.DAOErrorType.GENERIC_ERROR);
			}
		}
	}

	/**
	 * Deletes an album from the database by its ID, ensuring user authorization.
	 *
	 * @param idAlbum The ID of the album to delete.
	 * @param userId  The UUID of the user attempting the deletion (for
	 *                authorization).
	 * @return true if the deletion was successful, false otherwise.
	 * @throws DAOException if a database access error occurs or the user is not
	 *                      authorized.
	 */
	public boolean deleteAlbum(int idAlbum, UUID userId) throws DAOException {
		String query = "DELETE FROM Album WHERE idAlbum = ? AND idUser = UUID_TO_BIN(?)";
		try (PreparedStatement pStatement = connection.prepareStatement(query)) {
			pStatement.setInt(1, idAlbum);
			pStatement.setString(2, userId.toString());
			int affectedRows = pStatement.executeUpdate();
			if (affectedRows == 0) {
				// Could be not found OR not authorized
				throw new DAOException("Album with ID " + idAlbum + " not found for deletion or user not authorized.",
						DAOException.DAOErrorType.NOT_FOUND); // Or introduce UNAUTHORIZED type
			}
			return true; // If we reach here, affectedRows must be > 0
		} catch (SQLException e) {
			throw new DAOException("Error deleting album: " + e.getMessage(), e,
					DAOException.DAOErrorType.GENERIC_ERROR);
		}
	}
}
