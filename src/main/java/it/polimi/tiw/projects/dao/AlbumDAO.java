package it.polimi.tiw.projects.dao;

import it.polimi.tiw.projects.beans.Album;
import it.polimi.tiw.projects.exceptions.DAOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class AlbumDAO {
	private static final Logger logger = LoggerFactory.getLogger(AlbumDAO.class);
	private Connection connection;

	public AlbumDAO(Connection connection) {
		this.connection = connection;
	}

	/**
	 * Creates a new album in the database.
	 *
	 * @param name The name of the album.
	 * @param year The release year of the album.
	 * @param artist The artist of the album.
	 * @param image The path to the album's image file (can be null).
	 * @param idUser The UUID of the user creating the album.
	 * @return The newly created Album object with its generated ID.
	 * @throws DAOException if a database access error occurs
	 *         ({@link it.polimi.tiw.projects.exceptions.DAOException.DAOErrorType#GENERIC_ERROR})
	 *         or the name already exists for this user
	 *         ({@link it.polimi.tiw.projects.exceptions.DAOException.DAOErrorType#NAME_ALREADY_EXISTS}).
	 */
	public Album createAlbum(String name, int year, String artist, String image, UUID idUser)
			throws DAOException {
		logger.debug("Attempting to create album: name={}, year={}, artist={}, image={}, userId={}",
				name, year, artist, image, idUser);
		String query =
				"INSERT into Album (name, year, artist, image, idUser) VALUES(?, ?, ?, ?, UUID_TO_BIN(?))";
		Album newAlbum = null;

		try (PreparedStatement pStatement =
				connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
			pStatement.setString(1, name);
			pStatement.setInt(2, year);
			pStatement.setString(3, artist);
			// Handle potentially null image
			if (image != null) {
				pStatement.setString(4, image);
			} else {
				pStatement.setNull(4, Types.VARCHAR);
			}
			pStatement.setString(5, idUser.toString());
			int affectedRows = pStatement.executeUpdate();

			if (affectedRows == 0) {
				// ? This case might not happen with auto-increment keys but kept for robustness
				throw new DAOException("Creating album failed, no rows affected.",
						DAOException.DAOErrorType.GENERIC_ERROR);
			}

			try (ResultSet generatedKeys = pStatement.getGeneratedKeys()) {
				if (generatedKeys.next()) {
					int newId = generatedKeys.getInt(1);
					// Create the Album bean
					newAlbum = new Album();
					newAlbum.setIdAlbum(newId);
					newAlbum.setName(name);
					newAlbum.setYear(year);
					newAlbum.setArtist(artist);
					newAlbum.setImageFile(image);
					newAlbum.setIdUser(idUser);
					logger.info("Album created successfully with ID: {}", newAlbum.getIdAlbum());
				} else {
					logger.error("Creating album failed, no ID obtained for name={}, userId={}",
							name, idUser);
					throw new DAOException("Creating album failed, no ID obtained.",
							DAOException.DAOErrorType.GENERIC_ERROR);
				}
			}
		} catch (SQLException e) {
			if ("23000".equals(e.getSQLState())) {
				logger.warn(
						"Attempt to create album with existing name for user: name={}, userId={}. Details: {}",
						name, idUser, e.getMessage());
				throw new DAOException("Album name '" + name + "' already exists for this user.", e,
						DAOException.DAOErrorType.NAME_ALREADY_EXISTS);
			} else { // GENERIC_ERROR
				logger.error(
						"SQL error during album creation for name={}, userId={}: SQLState={}, Message={}",
						name, idUser, e.getSQLState(), e.getMessage(), e);
				throw new DAOException("Error creating album: " + e.getMessage(), e,
						DAOException.DAOErrorType.GENERIC_ERROR);
			}
		}
		return newAlbum;
	}

	/**
	 * Finds an album by its ID.
	 *
	 * @param idAlbum The ID of the album to find.
	 * @return The Album object if found.
	 * @throws DAOException if a database access error occurs
	 *         ({@link it.polimi.tiw.projects.exceptions.DAOException.DAOErrorType#GENERIC_ERROR})
	 *         or the album is not found
	 *         ({@link it.polimi.tiw.projects.exceptions.DAOException.DAOErrorType#NOT_FOUND}).
	 */
	public Album findAlbumById(int idAlbum) throws DAOException {
		logger.debug("Attempting to find album by ID: {}", idAlbum);
		Album album = null;
		String query =
				"SELECT idAlbum, name, year, artist, image, BIN_TO_UUID(idUser) as idUser FROM Album WHERE idAlbum = ?";
		try (PreparedStatement pStatement = connection.prepareStatement(query)) {
			pStatement.setInt(1, idAlbum);
			try (ResultSet result = pStatement.executeQuery()) {
				if (result.next()) {
					album = new Album();
					album.setIdAlbum(result.getInt("idAlbum"));
					album.setName(result.getString("name"));
					album.setYear(result.getInt("year"));
					album.setArtist(result.getString("artist"));
					album.setImageFile(result.getString("image"));
					album.setIdUser(UUID.fromString(result.getString("idUser")));
					logger.debug("Found album with ID: {}", idAlbum);
				} else {
					logger.warn("Album not found with ID: {}", idAlbum);
					throw new DAOException("Album with ID " + idAlbum + " not found.",
							DAOException.DAOErrorType.NOT_FOUND);
				}
			}
		} catch (SQLException e) {
			throw new DAOException("Error finding album by ID: " + e.getMessage(), e,
					DAOException.DAOErrorType.GENERIC_ERROR);
		} catch (IllegalArgumentException e) {
			throw new DAOException(
					"Error finding album by ID due to invalid argument: " + e.getMessage(), e,
					DAOException.DAOErrorType.GENERIC_ERROR);
		}
		// If we reach here, album must have been found and populated
		return album;
	}

	/**
	 * Finds all albums in the database.
	 *
	 * @return A list of all albums.
	 * @throws DAOException if a database access error occurs
	 *         ({@link it.polimi.tiw.projects.exceptions.DAOException.DAOErrorType#GENERIC_ERROR}).
	 */
	public List<Album> findAllAlbums() throws DAOException {
		logger.debug("Attempting to find all albums");
		List<Album> albums = new ArrayList<>();
		String query =
				"SELECT idAlbum, name, year, artist, image, BIN_TO_UUID(idUser) as idUser FROM Album ORDER BY artist, year, name";
		try (Statement statement = connection.createStatement();
				ResultSet result = statement.executeQuery(query)) {
			while (result.next()) {
				Album album = new Album();
				album.setIdAlbum(result.getInt("idAlbum"));
				album.setName(result.getString("name"));
				album.setYear(result.getInt("year"));
				album.setArtist(result.getString("artist"));
				album.setImageFile(result.getString("image"));
				album.setIdUser(UUID.fromString(result.getString("idUser")));
				albums.add(album);
			}
			logger.debug("Found {} albums", albums.size());
		} catch (SQLException e) {
			logger.error("SQL error finding all albums: {}", e.getMessage(), e);
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
	 * @throws DAOException if a database access error occurs
	 *         ({@link it.polimi.tiw.projects.exceptions.DAOException.DAOErrorType#GENERIC_ERROR}).
	 */
	public List<Album> findAlbumsByUser(UUID userId) throws DAOException {
		logger.debug("Attempting to find albums for user ID: {}", userId);
		List<Album> userAlbums = new ArrayList<>();
		String query =
				"SELECT idAlbum, name, year, artist, image, BIN_TO_UUID(idUser) as idUser FROM Album WHERE idUser = UUID_TO_BIN(?) ORDER BY year, name";

		try (PreparedStatement pStatement = connection.prepareStatement(query)) {
			pStatement.setString(1, userId.toString());
			try (ResultSet result = pStatement.executeQuery()) {
				while (result.next()) {
					Album album = new Album();
					album.setIdAlbum(result.getInt("idAlbum"));
					album.setName(result.getString("name"));
					album.setYear(result.getInt("year"));
					album.setArtist(result.getString("artist"));
					album.setImageFile(result.getString("image"));
					album.setIdUser(UUID.fromString(result.getString("idUser")));
					userAlbums.add(album);
				}
				logger.debug("Found {} albums for user ID: {}", userAlbums.size(), userId);
			}
		} catch (SQLException e) {
			logger.error("SQL error finding albums for user ID {}: {}", userId, e.getMessage(), e);
			throw new DAOException("Error finding albums by user: " + e.getMessage(), e,
					DAOException.DAOErrorType.GENERIC_ERROR);
		}
		return userAlbums;
	}

	/**
	 * Updates an existing album in the database, only modifying fields with non-null values.
	 *
	 * @param idAlbum The ID of the album to update.
	 * @param userId The UUID of the user attempting the update (for authorization).
	 * @param name The new name for the album (or null to keep existing).
	 * @param year The new release year for the album (or null to keep existing).
	 * @param artist The new artist for the album (or null to keep existing).
	 * @param image The new image path for the album (or null to keep existing).
	 * @throws DAOException if a database access error occurs
	 *         ({@link it.polimi.tiw.projects.exceptions.DAOException.DAOErrorType#GENERIC_ERROR}),
	 *         the album is not found or the user is not authorized
	 *         ({@link it.polimi.tiw.projects.exceptions.DAOException.DAOErrorType#NOT_FOUND}), or
	 *         the new name already exists for this user
	 *         ({@link it.polimi.tiw.projects.exceptions.DAOException.DAOErrorType#NAME_ALREADY_EXISTS}).
	 * @throws IllegalArgumentException if all update parameters (name, year, artist, image) are
	 *         null.
	 */
	public void updateAlbum(int idAlbum, UUID userId, String name, Integer year, String artist,
			String image) throws DAOException {
		logger.debug(
				"Attempting to update album ID: {} for user ID: {} with data: name={}, year={}, artist={}, image={}",
				idAlbum, userId, name, year, artist, image);
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
		if (image != null) {
			if (!firstField)
				queryBuilder.append(", ");
			queryBuilder.append("image = ?");
			params.add(image);
			firstField = false;
		}

		// Check if any field was actually added for update
		if (params.isEmpty()) {
			logger.warn("Update attempt for album ID {} failed: No fields provided for update.",
					idAlbum);
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
				// * Note: We don't handle setNull here because the update logic only adds
				// * non-null parameters to the list. If a user wants to set image to NULL,
				// * they would need a different mechanism or a specific value indicating NULL.
				// * For now, this update only sets non-null values.
			}

			int affectedRows = pStatement.executeUpdate();
			if (affectedRows == 0) {
				logger.warn("Update failed for album ID {}: Not found or user {} not authorized.",
						idAlbum, userId);
				// We throw NOT_FOUND here, but it could also be ACCESS_DENIED. The DB doesn't
				// distinguish.
				throw new DAOException(
						"Album with ID " + idAlbum
								+ " not found for update or user not authorized.",
						DAOException.DAOErrorType.NOT_FOUND);
			}
			logger.info("Album ID {} updated successfully by user {}", idAlbum, userId);
			// No return value needed, success is indicated by lack of exception
		} catch (SQLException e) {
			if ("23000".equals(e.getSQLState()) && name != null) {
				logger.warn(
						"Attempt to update album ID {} with existing name for user {}: name={}, userId={}. Details: {}",
						idAlbum, userId, name, userId, e.getMessage());
				throw new DAOException("Album name '" + name + "' already exists for this user.", e,
						DAOException.DAOErrorType.NAME_ALREADY_EXISTS);
			} else { // GENERIC_ERROR
				logger.error("SQL error updating album ID {} for user {}: SQLState={}, Message={}",
						idAlbum, userId, e.getSQLState(), e.getMessage(), e);
				throw new DAOException("Error updating album: " + e.getMessage(), e,
						DAOException.DAOErrorType.GENERIC_ERROR);
			}
		}
	}

	/**
	 * Deletes an album from the database by its ID, ensuring user authorization.
	 *
	 * @param idAlbum The ID of the album to delete.
	 * @param userId The UUID of the user attempting the deletion (for authorization).
	 * @throws DAOException if a database access error occurs
	 *         ({@link it.polimi.tiw.projects.exceptions.DAOException.DAOErrorType#GENERIC_ERROR}),
	 *         the album is not found, or the user is not authorized
	 *         ({@link it.polimi.tiw.projects.exceptions.DAOException.DAOErrorType#NOT_FOUND}).
	 */
	public void deleteAlbum(int idAlbum, UUID userId) throws DAOException {
		logger.debug("Attempting to delete album ID: {} by user ID: {}", idAlbum, userId);
		String query = "DELETE FROM Album WHERE idAlbum = ? AND idUser = UUID_TO_BIN(?)";
		try (PreparedStatement pStatement = connection.prepareStatement(query)) {
			pStatement.setInt(1, idAlbum);
			pStatement.setString(2, userId.toString());
			int affectedRows = pStatement.executeUpdate();
			if (affectedRows == 0) {
				logger.warn("Delete failed for album ID {}: Not found or user {} not authorized.",
						idAlbum, userId);
				// We throw NOT_FOUND here, but it could also be ACCESS_DENIED. The DB doesn't
				// distinguish.
				throw new DAOException(
						"Album with ID " + idAlbum
								+ " not found for deletion or user not authorized.",
						DAOException.DAOErrorType.NOT_FOUND);
			}
			logger.info("Album ID {} deleted successfully by user {}", idAlbum, userId);
			// No return value needed, success is indicated by lack of exception
		} catch (SQLException e) {
			logger.error("SQL error deleting album ID {} by user {}: {}", idAlbum, userId,
					e.getMessage(), e);
			throw new DAOException("Error deleting album: " + e.getMessage(), e,
					DAOException.DAOErrorType.GENERIC_ERROR);
		}
	}
}
