package it.polimi.tiw.projects.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.polimi.tiw.projects.beans.Playlist;
import it.polimi.tiw.projects.exceptions.DAOException;
import it.polimi.tiw.projects.exceptions.DAOException.DAOErrorType;

public class PlaylistDAO {
	private static final Logger logger = LoggerFactory.getLogger(PlaylistDAO.class);
	private Connection connection;

	public PlaylistDAO(Connection connection) {
		this.connection = connection;
	}

	/**
	 * Creates a new playlist in the database, including its metadata and associated
	 * songs. Uses a transaction to ensure atomicity.
	 *
	 * @param name    The name of the playlist.
	 * @param image   The path or URL to the playlist image (can be null).
	 * @param idUser  The UUID of the user creating the playlist.
	 * @param songIds A list of song IDs to include in the playlist.
	 *
	 * @return The generated Playlist
	 * @throws SQLException if a database access error occurs that isn't handled by
	 *                      DAOException.
	 * @throws DAOException if the playlist name already exists for the user or
	 *                      another DAO-specific error occurs.
	 */
	public Playlist createPlaylist(String name, String image, UUID idUser, List<Integer> songIds)
			throws SQLException, DAOException {
		logger.debug("Attempting to create playlist: name={}, image={}, userId={}, songCount={}", name, image, idUser,
				songIds != null ? songIds.size() : 0);
		String insertMetadataSQL = "INSERT INTO `playlist-metadata` (name, image, idUser) VALUES (?, ?, UUID_TO_BIN(?))";
		String insertContentSQL = "INSERT INTO `playlist-content` (idPlaylist, idSong) VALUES (?, ?)";
		// Check name against the new table name
		String checkNameSQL = "SELECT idPlaylist FROM `playlist-metadata` WHERE name = ? AND idUser = UUID_TO_BIN(?)";

		int newPlaylistId = -1;

		boolean previousAutoCommit = connection.getAutoCommit();

		try {
			connection.setAutoCommit(false); // Start transaction

			// Check if playlist name already exists for this user
			try (PreparedStatement pStatementCheck = connection.prepareStatement(checkNameSQL)) {
				pStatementCheck.setString(1, name);
				pStatementCheck.setString(2, idUser.toString());
				try (ResultSet checkResult = pStatementCheck.executeQuery()) {
					if (checkResult.next()) {
						logger.warn("Playlist creation failed: Name '{}' already exists for user {}", name, idUser);
						throw new DAOException("Playlist name '" + name + "' already exists for this user.",
								DAOErrorType.NAME_ALREADY_EXISTS);
					}
				}
			} catch (SQLException e) {
				logger.error("SQL error checking playlist name existence for name={}, userId={}: {}", name, idUser,
						e.getMessage(), e);
				throw new DAOException("Database error checking playlist name.", e, DAOErrorType.GENERIC_ERROR);
			}

			// Insert playlist metadata
			try (PreparedStatement pStatementMetadata = connection.prepareStatement(insertMetadataSQL,
					Statement.RETURN_GENERATED_KEYS)) {
				pStatementMetadata.setString(1, name);
				pStatementMetadata.setString(2, image);
				pStatementMetadata.setString(3, idUser.toString());

				int affectedRows = pStatementMetadata.executeUpdate();

				if (affectedRows == 0) {
					logger.error("Creating playlist metadata failed, no rows affected for name={}, userId={}", name,
							idUser);
					connection.rollback(); // Rollback before throwing
					throw new SQLException("Creating playlist metadata failed, no rows affected.");
				}
				try (ResultSet generatedKeys = pStatementMetadata.getGeneratedKeys()) {
					if (generatedKeys.next()) {
						newPlaylistId = generatedKeys.getInt(1);
						logger.debug("Playlist metadata created with ID: {}", newPlaylistId);
					} else {
						logger.error("Creating playlist metadata failed, no ID obtained for name={}, userId={}", name,
								idUser);
						connection.rollback(); // Rollback before throwing
						throw new SQLException("Creating playlist metadata failed, no ID obtained.");
					}
				}
			} catch (SQLException e) {
				logger.error("SQL error inserting playlist metadata for name={}, userId={}: {}", name, idUser,
						e.getMessage(), e);
				connection.rollback(); // Ensure rollback on metadata insertion failure
				throw e; // Re-throw to be handled by the outer catch block
			}

			// Insert playlist content (songs)
			if (songIds != null && !songIds.isEmpty()) {
				try (PreparedStatement pStatementContent = connection.prepareStatement(insertContentSQL)) {
					for (Integer songId : songIds) {
						pStatementContent.setInt(1, newPlaylistId);
						pStatementContent.setInt(2, songId);
						pStatementContent.addBatch();
					}
					pStatementContent.executeBatch();
					logger.debug("Added {} songs to playlist ID: {}", songIds.size(), newPlaylistId);
				} catch (SQLException e) {
					logger.error("SQL error adding songs to playlist ID {}: {}", newPlaylistId, e.getMessage(), e);
					connection.rollback(); // Ensure rollback on content insertion failure
					throw e; // Re-throw to be handled by the outer catch block
				}
			} else {
				logger.debug("No songs provided for playlist ID: {}", newPlaylistId);
			}

			connection.commit(); // Commit transaction
			logger.info("Playlist ID {} created successfully for user {}", newPlaylistId, idUser);

		} catch (SQLException e) {
			logger.warn("Transaction rolled back for playlist creation (name={}, userId={}) due to SQL error: {}", name,
					idUser, e.getMessage());
			try {
				connection.rollback(); // Rollback transaction on error
			} catch (SQLException ex) {
				logger.error("Rollback failed during playlist creation error handling: {}", ex.getMessage(), ex);
			}

			// Handle specific constraint violations with DAOException
			logger.error(
					"SQL error during playlist creation transaction for name={}, userId={}: SQLState={}, Message={}",
					name, idUser, e.getSQLState(), e.getMessage(), e);
			if ("23000".equals(e.getSQLState())) { // Integrity constraint violation
				if (e.getMessage().contains("unique_playlist_per_user")) {
					throw new DAOException("Playlist name '" + name + "' already exists for this user.", e,
							DAOErrorType.NAME_ALREADY_EXISTS);
				} else if (e.getMessage().contains("unique_playlist_and_song")) {
					// ? Decide if this is NAME_ALREADY_EXISTS or GENERIC_ERROR. Let's use GENERIC
					// ? for now.
					throw new DAOException("Duplicate song ID found in the playlist.", e, DAOErrorType.GENERIC_ERROR);
				} else if (e.getMessage().contains("fk_playlist-content_1")) {
					throw new DAOException("One or more song IDs do not exist.", e, DAOErrorType.NOT_FOUND);
				}
			}
			// Wrap other SQL exceptions
			throw new DAOException("Database error during playlist creation.", e, DAOErrorType.GENERIC_ERROR);

		} finally {
			if (connection != null) {
				try {
					connection.setAutoCommit(previousAutoCommit); // Restore to previous auto-commit behavior
				} catch (SQLException e) {
					// System.err.println("Failed to restore auto-commit: " + e.getMessage());
					logger.error("Failed to restore auto-commit state after playlist creation attempt: {}",
							e.getMessage(), e);
				}
			}
		}
		return this.findPlaylistById(newPlaylistId, idUser);
	}

	/**
	 * Finds the IDs of all playlists created by a specific user.
	 *
	 * @param idUser The UUID of the user.
	 * @return A list of playlist IDs.
	 * @throws DAOException if a database access error occurs.
	 */
	public List<Integer> findPlaylistIdsByUser(UUID idUser) throws DAOException {
		logger.debug("Attempting to find playlist IDs for user ID: {}", idUser);
		List<Integer> playlistIds = new ArrayList<>();
		String query = "SELECT idPlaylist FROM `playlist-metadata` WHERE idUser = UUID_TO_BIN(?)";

		try (PreparedStatement pStatement = connection.prepareStatement(query)) {
			pStatement.setString(1, idUser.toString());
			try (ResultSet resultSet = pStatement.executeQuery()) {
				while (resultSet.next()) {
					playlistIds.add(resultSet.getInt("idPlaylist"));
				}
				logger.debug("Found {} playlist IDs for user ID: {}", playlistIds.size(), idUser);
			}
		} catch (SQLException e) {
			logger.error("SQL error finding playlist IDs for user ID {}: {}", idUser, e.getMessage(), e);
			throw new DAOException("Database error finding playlist IDs by user.", e, DAOErrorType.GENERIC_ERROR);
		}
		return playlistIds;
	}

	/**
	 * Finds a specific playlist by its ID, including its list of song IDs. Verifies
	 * ownership using the provided user ID. Uses BIN_TO_UUID and UUID_TO_BIN
	 * appropriately.
	 *
	 * @param playlistId The ID of the playlist to find.
	 * @param userId     The UUID of the user who owns the playlist (for
	 *                   verification).
	 * @return The Playlist object if found and owned by the user, null otherwise.
	 * @throws DAOException if a database access error occurs.
	 */
	public Playlist findPlaylistById(int playlistId, UUID userId) throws DAOException {
		logger.debug("Attempting to find playlist ID: {} for user ID: {}", playlistId, userId);
		Playlist playlist = null;
		String queryMetadata = "SELECT name, birthday, image, BIN_TO_UUID(idUser) as userUUID FROM `playlist-metadata` WHERE idPlaylist = ? AND idUser = UUID_TO_BIN(?)";
		String queryContent = "SELECT idSong FROM `playlist-content` WHERE idPlaylist = ?";

		try (PreparedStatement pStatementMetadata = connection.prepareStatement(queryMetadata)) {
			pStatementMetadata.setInt(1, playlistId);
			pStatementMetadata.setString(2, userId.toString());
			try (ResultSet rsMetadata = pStatementMetadata.executeQuery()) {
				if (rsMetadata.next()) {
					playlist = new Playlist();
					playlist.setIdPlaylist(playlistId);
					playlist.setName(rsMetadata.getString("name"));
					playlist.setBirthday(rsMetadata.getTimestamp("birthday"));
					playlist.setImage(rsMetadata.getString("image"));
					playlist.setIdUser(UUID.fromString(rsMetadata.getString("userUUID")));

					// Fetch song IDs
					List<Integer> songIds = new ArrayList<>();
					try (PreparedStatement pStatementContent = connection.prepareStatement(queryContent)) {
						pStatementContent.setInt(1, playlistId);
						try (ResultSet rsContent = pStatementContent.executeQuery()) {
							while (rsContent.next()) {
								songIds.add(rsContent.getInt("idSong"));
							}
						}
						logger.debug("Found {} songs for playlist ID: {}", songIds.size(), playlistId);
					} catch (SQLException e) {
						logger.error("SQL error fetching songs for playlist ID {}: {}", playlistId, e.getMessage(), e);
						throw new DAOException("Database error fetching songs for playlist.", e,
								DAOErrorType.GENERIC_ERROR); // Re-throw wrapped
					}
					playlist.setSongs(songIds);
					logger.debug("Found playlist ID: {} owned by user ID: {}", playlistId, userId);
				} else {
					logger.debug("Playlist ID: {} not found or not owned by user ID: {}", playlistId, userId);
					// * If rsMetadata.next() is false, the playlist doesn't exist or doesn't belong
					// * to the user, return null
				}
			}
		} catch (SQLException e) {
			logger.error("SQL error finding playlist metadata for ID {}: {}", playlistId, e.getMessage(), e);
			throw new DAOException("Database error finding playlist by ID.", e, DAOErrorType.GENERIC_ERROR);
		} catch (IllegalArgumentException e) {
			// Catch potential UUID parsing errors
			logger.error("Error parsing UUID from database for playlist ID {}: {}", playlistId, e.getMessage(), e);
			throw new DAOException("Error parsing UUID from database.", e, DAOErrorType.GENERIC_ERROR);
		}
		return playlist;
	}

	/**
	 * Deletes a specific playlist owned by a user. Relies on the database's ON
	 * DELETE CASCADE constraint to remove associated songs from the
	 * `playlist-content` table.
	 *
	 * @param playlistId The ID of the playlist to delete.
	 * @param userId     The UUID of the user who must own the playlist.
	 * @return true if the playlist was found and deleted, false otherwise.
	 * @throws DAOException if a database access error occurs.
	 */
	public boolean deletePlaylist(int playlistId, UUID userId) throws DAOException {
		logger.debug("Attempting to delete playlist ID: {} by user ID: {}", playlistId, userId);
		String deleteQuery = "DELETE FROM `playlist-metadata` WHERE idPlaylist = ? AND idUser = UUID_TO_BIN(?)";
		int affectedRows = 0;

		try (PreparedStatement pStatement = connection.prepareStatement(deleteQuery)) {
			pStatement.setInt(1, playlistId);
			pStatement.setString(2, userId.toString());
			affectedRows = pStatement.executeUpdate();

			if (affectedRows > 0) {
				logger.info("Playlist ID {} deleted successfully by user {}", playlistId, userId);
				return true;
			} else {
				logger.warn("Delete failed for playlist ID {}: Not found or user {} not authorized.", playlistId,
						userId);
				return false;
			}

		} catch (SQLException e) {
			logger.error("SQL error deleting playlist ID {} by user {}: {}", playlistId, userId, e.getMessage(), e);
			// ? This could indicate a problem with the DELETE statement itself or
			// ? potentially an issue during the cascade operation if
			// ? constraints/permissions interfere.
			throw new DAOException("Database error deleting playlist metadata for ID: " + playlistId, e,
					DAOErrorType.GENERIC_ERROR);
		}
	}

	/**
	 * Adds a song to a specific playlist owned by a user. Checks if the playlist
	 * exists and belongs to the user before adding the song.
	 *
	 * @param playlistId The ID of the playlist to add the song to.
	 * @param userId     The UUID of the user who must own the playlist.
	 * @param songId     The ID of the song to add.
	 * @return true if the song was added successfully, false if the playlist was
	 *         not found or doesn't belong to the user.
	 * @throws DAOException if a database access error occurs.
	 */
	public boolean addSongToPlaylist(int playlistId, UUID userId, int songId) throws DAOException {
		logger.debug("Attempting to add song ID: {} to playlist ID: {} by user ID: {}", songId, playlistId, userId);
		String checkOwnershipQuery = "SELECT * FROM `playlist-metadata` WHERE idPlaylist = ? AND idUser = UUID_TO_BIN(?)";
		String insertQuery = "INSERT INTO `playlist-content` (idPlaylist, idSong) VALUES (?, ?)";
		int affectedRows = 0;

		// Check ownership
		try (PreparedStatement pStatement = connection.prepareStatement(checkOwnershipQuery)) {
			pStatement.setInt(1, playlistId);
			pStatement.setString(2, userId.toString());
			try (ResultSet rs = pStatement.executeQuery()) {
				if (!rs.next()) {
					logger.warn("Add song failed: Playlist ID {} not found or not owned by user {}", playlistId,
							userId);
					return false; // Playlist not found or doesn't belong to user.
				}
			}
		} catch (SQLException e) {
			logger.error("SQL error checking ownership before adding song {} to playlist {}: {}", songId, playlistId,
					e.getMessage(), e);
			throw new DAOException("Database error checking ownership.", e, DAOErrorType.GENERIC_ERROR);
		}

		// Insert song into playlist
		try (PreparedStatement pStatement = connection.prepareStatement(insertQuery)) {
			pStatement.setInt(1, playlistId);
			pStatement.setInt(2, songId);
			affectedRows = pStatement.executeUpdate();
			if (affectedRows > 0) {
				logger.info("Song ID {} added successfully to playlist ID {} by user {}", songId, playlistId, userId);
			} else {
				// ? This case should ideally not happen if ownership check passed and no
				// ? constraint violation occurred, but log just in case.
				logger.warn("Adding song {} to playlist {} returned 0 affected rows unexpectedly.", songId, playlistId);
			}
		} catch (SQLException e) {
			// Handle potential constraint violations (e.g., duplicate song in playlist,
			// non-existent song ID)
			if ("23000".equals(e.getSQLState())) { // Integrity constraint violation
				if (e.getMessage().contains("unique_playlist_and_song")) {
					logger.warn("Attempt to add duplicate song ID {} to playlist ID {}", songId, playlistId);
					// Depending on requirements, you might return false or throw a specific
					// DAOException
					return false;
				} else if (e.getMessage().contains("fk_playlist-content_1")) { // Assuming this is the FK for song ID
					logger.warn("Attempt to add non-existent song ID {} to playlist ID {}", songId, playlistId);
					throw new DAOException("Song ID " + songId + " does not exist.", e, DAOErrorType.NOT_FOUND);
				}
			}
			logger.error("SQL error adding song {} to playlist {}: {}", songId, playlistId, e.getMessage(), e);
			throw new DAOException("Database error adding song to playlist.", e, DAOErrorType.GENERIC_ERROR);
		}
		return affectedRows > 0;
	}

	/**
	 * Removes a song from a specific playlist owned by a user. Checks if the
	 * playlist exists and belongs to the user before removing the song.
	 *
	 * @param playlistId The ID of the playlist to remove the song from.
	 * @param userId     The UUID of the user who must own the playlist.
	 * @param songId     The ID of the song to remove.
	 * @return true if the song was removed successfully, false if the playlist was
	 *         not found or doesn't belong to the user.
	 * @throws DAOException if a database access error occurs.
	 */
	public boolean removeSongFromPlaylist(int playlistId, UUID userId, int songId) throws DAOException {
		logger.debug("Attempting to remove song ID: {} from playlist ID: {} by user ID: {}", songId, playlistId,
				userId);
		String checkOwnershipQuery = "SELECT * FROM `playlist-metadata` WHERE idPlaylist = ? AND idUser = UUID_TO_BIN(?)";
		String deleteQuery = "DELETE FROM `playlist-content` WHERE idPlaylist = ? AND idSong = ?";
		int affectedRows = 0;

		// Check ownership
		try (PreparedStatement pStatement = connection.prepareStatement(checkOwnershipQuery)) {
			pStatement.setInt(1, playlistId);
			pStatement.setString(2, userId.toString());
			try (ResultSet rs = pStatement.executeQuery()) {
				if (!rs.next()) {
					logger.warn("Remove song failed: Playlist ID {} not found or not owned by user {}", playlistId,
							userId);
					return false; // Playlist not found or doesn't belong to user.
				}
			}

		} catch (SQLException e) {
			logger.error("SQL error checking ownership before removing song {} from playlist {}: {}", songId,
					playlistId, e.getMessage(), e);
			throw new DAOException("Database error checking ownership", e, DAOErrorType.GENERIC_ERROR);
		}

		// Delete Song from Playlist
		try (PreparedStatement pStatement = connection.prepareStatement(deleteQuery)) {
			pStatement.setInt(1, playlistId);
			pStatement.setInt(2, songId);
			affectedRows = pStatement.executeUpdate();
			if (affectedRows > 0) {
				logger.info("Song ID {} removed successfully from playlist ID {} by user {}", songId, playlistId,
						userId);
			} else {
				// ? This implies the song wasn't in the playlist, or the playlist/user check
				// ? somehow failed between steps (unlikely)
				logger.warn(
						"Removing song {} from playlist {} returned 0 affected rows (song might not have been present).",
						songId, playlistId);
			}
		} catch (SQLException e) {
			logger.error("SQL error removing song {} from playlist {}: {}", songId, playlistId, e.getMessage(), e);
			throw new DAOException("Database error removing song from playlist.", e, DAOErrorType.GENERIC_ERROR);
		}
		return affectedRows > 0;
	}

}
