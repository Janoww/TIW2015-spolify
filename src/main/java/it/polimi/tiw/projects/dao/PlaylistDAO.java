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
	 * @param idUser  The UUID of the user creating the playlist.
	 * @param songIds A list of song IDs to include in the playlist.
	 *
	 * @return The generated Playlist
	 * @throws SQLException if a database access error occurs that isn't handled by
	 *                      DAOException.
	 * @throws DAOException if the playlist name already exists for the user
	 *                      ({@link DAOErrorType#NAME_ALREADY_EXISTS}), a provided
	 *                      song ID is not found ({@link DAOErrorType#NOT_FOUND}), a
	 *                      constraint violation occurs (e.g., null song ID)
	 *                      ({@link DAOErrorType#CONSTRAINT_VIOLATION}), a duplicate
	 *                      song ID is provided in the input list
	 *                      ({@link DAOErrorType#DUPLICATE_ENTRY}), or another
	 *                      database error occurs
	 *                      ({@link DAOErrorType#GENERIC_ERROR}).
	 */
	public Playlist createPlaylist(String name, UUID idUser, List<Integer> songIds) throws SQLException, DAOException {
		logger.debug("Attempting to create playlist: name={}, userId={}, songCount={}", name, idUser,
				songIds != null ? songIds.size() : 0);
		String checkSongExistsSQL = "SELECT 1 FROM Song WHERE idSong = ?";
		String insertMetadataSQL = "INSERT INTO playlist_metadata (name, idUser) VALUES (?, UUID_TO_BIN(?))";
		String insertContentSQL = "INSERT INTO playlist_content (idPlaylist, idSong) VALUES (?, ?)";
		String checkNameSQL = "SELECT idPlaylist FROM playlist_metadata WHERE name = ? AND idUser = UUID_TO_BIN(?)";

		int newPlaylistId = -1;

		boolean previousAutoCommit = connection.getAutoCommit();

		try {
			connection.setAutoCommit(false); // Start transaction

			// 1. Check if playlist name already exists for this user
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

			// 2. Check if all provided song IDs exist (only if songs are provided)
			if (songIds != null && !songIds.isEmpty()) {
				try (PreparedStatement pStatementCheckSong = connection.prepareStatement(checkSongExistsSQL)) {
					for (Integer songId : songIds) {
						if (songId == null) {
							throw new DAOException("Playlist cannot contain null song IDs.",
									DAOErrorType.CONSTRAINT_VIOLATION);
						}
						pStatementCheckSong.setInt(1, songId);
						try (ResultSet rsSong = pStatementCheckSong.executeQuery()) {
							if (!rsSong.next()) {
								logger.warn("Playlist creation failed: Song ID {} does not exist.", songId);
								throw new DAOException("Song with ID " + songId + " not found.",
										DAOErrorType.NOT_FOUND);
							}
						}
						pStatementCheckSong.clearParameters(); // Clear parameters for the next iteration
					}
					logger.debug("All {} provided song IDs verified.", songIds.size());
				} catch (SQLException e) {
					logger.error("SQL error verifying song existence for playlist creation (name={}): {}", name,
							e.getMessage(), e);
					throw new DAOException("Database error verifying song existence.", e, DAOErrorType.GENERIC_ERROR);
				}
			}

			// 3. Insert playlist metadata
			try (PreparedStatement pStatementMetadata = connection.prepareStatement(insertMetadataSQL,
					Statement.RETURN_GENERATED_KEYS)) {
				pStatementMetadata.setString(1, name);
				pStatementMetadata.setString(2, idUser.toString());

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

			// 4. Insert playlist content (songs) - Skip existence check here as it was done
			// before metadata insertion
			if (songIds != null && !songIds.isEmpty()) {
				try (PreparedStatement pStatementContent = connection.prepareStatement(insertContentSQL)) {
					for (Integer songId : songIds) {
						// songId is guaranteed non-null and existing from the check above
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
			if ("23000".equals(e.getSQLState())) { // Integrity constraint violation
				if (e.getMessage().contains("unique_playlist_per_user")) {
					logger.warn("Playlist creation failed for name={}, userId={}: Name already exists. Details: {}",
							name, idUser, e.getMessage());
					throw new DAOException("Playlist name '" + name + "' already exists for this user.", e,
							DAOErrorType.NAME_ALREADY_EXISTS);
				} else if (e.getMessage().contains("unique_playlist_and_song")) {
					logger.warn(
							"Playlist creation failed for name={}, userId={}: Duplicate song ID in input. Details: {}",
							name, idUser, e.getMessage());
					throw new DAOException("Duplicate song ID found in the input list for the playlist.", e,
							DAOErrorType.DUPLICATE_ENTRY);
				} else if (e.getMessage().contains("fk_playlist-content_1")) {
					logger.error(
							"Unexpected FK violation for playlist_content despite pre-check. PlaylistID={}, SQLState={}, Message={}",
							newPlaylistId, e.getSQLState(), e.getMessage(), e);
					throw new DAOException("Unexpected error associating songs with playlist.", e,
							DAOErrorType.CONSTRAINT_VIOLATION);
				} else { // Other 23000 errors
					logger.error(
							"SQL integrity constraint violation during playlist creation for name={}, userId={}: SQLState={}, Message={}",
							name, idUser, e.getSQLState(), e.getMessage(), e);
					throw new DAOException("Database integrity constraint violation during playlist creation.", e,
							DAOErrorType.CONSTRAINT_VIOLATION);
				}
			} else { // Other SQL exceptions (not 23000)
				logger.error(
						"SQL error during playlist creation transaction for name={}, userId={}: SQLState={}, Message={}",
						name, idUser, e.getSQLState(), e.getMessage(), e);
				throw new DAOException("Database error during playlist creation.", e, DAOErrorType.GENERIC_ERROR);
			}

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
	 * @throws DAOException if a database access error occurs
	 *                      ({@link DAOErrorType#GENERIC_ERROR}).
	 */
	public List<Integer> findPlaylistIdsByUser(UUID idUser) throws DAOException {
		logger.debug("Attempting to find playlist IDs for user ID: {}", idUser);
		List<Integer> playlistIds = new ArrayList<>();
		String query = "SELECT idPlaylist FROM playlist_metadata WHERE idUser = UUID_TO_BIN(?)";

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
	 * @param userId     The UUID of the user who must own the playlist (for
	 *                   verification).
	 * @return The Playlist object if found and owned by the user.
	 * @throws DAOException if the playlist is not found
	 *                      ({@link DAOErrorType#NOT_FOUND}), the user is not
	 *                      authorized to access it
	 *                      ({@link DAOErrorType#ACCESS_DENIED}), or another
	 *                      database error occurs
	 *                      ({@link DAOErrorType#GENERIC_ERROR}).
	 */
	public Playlist findPlaylistById(int playlistId, UUID userId) throws DAOException {
		logger.debug("Attempting to find playlist ID: {} for user ID: {}", playlistId, userId);
		Playlist playlist = null;
		// First check if playlist exists at all
		String checkExistenceQuery = "SELECT BIN_TO_UUID(idUser) as ownerUUID FROM playlist_metadata WHERE idPlaylist = ?";
		String queryMetadata = "SELECT name, birthday, BIN_TO_UUID(idUser) as userUUID FROM playlist_metadata WHERE idPlaylist = ? AND idUser = UUID_TO_BIN(?)";
		String queryContent = "SELECT idSong FROM playlist_content WHERE idPlaylist = ?";

		try {
			// 1. Check Existence and Ownership
			UUID ownerUUID = null;
			try (PreparedStatement pStatementCheck = connection.prepareStatement(checkExistenceQuery)) {
				pStatementCheck.setInt(1, playlistId);
				try (ResultSet rsCheck = pStatementCheck.executeQuery()) {
					if (rsCheck.next()) {
						ownerUUID = UUID.fromString(rsCheck.getString("ownerUUID"));
					} else {
						logger.warn("Playlist ID: {} not found.", playlistId);
						throw new DAOException("Playlist with ID " + playlistId + " not found.",
								DAOErrorType.NOT_FOUND);
					}
				}
			} catch (SQLException e) {
				logger.error("SQL error checking playlist existence for ID {}: {}", playlistId, e.getMessage(), e);
				throw new DAOException("Database error checking playlist existence.", e, DAOErrorType.GENERIC_ERROR);
			}

			// 2. Verify Ownership
			if (!ownerUUID.equals(userId)) {
				logger.warn("User ID {} attempted to access playlist ID {} owned by {}", userId, playlistId, ownerUUID);
				throw new DAOException("User not authorized to access playlist ID " + playlistId,
						DAOErrorType.ACCESS_DENIED);
			}

			// 3. Fetch Full Playlist Details (since existence and ownership are confirmed)
			try (PreparedStatement pStatementMetadata = connection.prepareStatement(queryMetadata)) {
				pStatementMetadata.setInt(1, playlistId);
				pStatementMetadata.setString(2, userId.toString()); // Redundant check, but keeps query logic clear
				try (ResultSet rsMetadata = pStatementMetadata.executeQuery()) {
					if (rsMetadata.next()) { // Should always be true if we passed the checks
						playlist = new Playlist();
						playlist.setIdPlaylist(playlistId);
						playlist.setName(rsMetadata.getString("name"));
						playlist.setBirthday(rsMetadata.getTimestamp("birthday"));
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
							logger.error("SQL error fetching songs for playlist ID {}: {}", playlistId, e.getMessage(),
									e);
							throw new DAOException("Database error fetching songs for playlist.", e,
									DAOErrorType.GENERIC_ERROR); // Re-throw wrapped
						}
						playlist.setSongs(songIds);
						logger.debug("Successfully retrieved playlist ID: {} owned by user ID: {}", playlistId, userId);
					} else {
						// This case should theoretically not be reached if existence/ownership checks
						// passed
						logger.error("Inconsistency: Playlist ID {} passed checks but metadata query failed.",
								playlistId);
						throw new DAOException("Inconsistent state retrieving playlist metadata.",
								DAOErrorType.GENERIC_ERROR);
					}
				}
			} catch (SQLException e) {
				logger.error("SQL error finding playlist metadata for ID {}: {}", playlistId, e.getMessage(), e);
				throw new DAOException("Database error finding playlist by ID.", e, DAOErrorType.GENERIC_ERROR);
			}
		} catch (IllegalArgumentException e) {
			// Catch potential UUID parsing errors
			logger.error("Error parsing UUID from database for playlist ID {}: {}", playlistId, e.getMessage());
			throw new DAOException("Error parsing UUID from database.", e, DAOErrorType.GENERIC_ERROR);
		}
		return playlist;
	}

	/**
	 * Deletes a specific playlist owned by a user. Relies on the database's ON
	 * DELETE CASCADE constraint to remove associated songs from the
	 * `playlist_content` table.
	 *
	 * @param playlistId The ID of the playlist to delete.
	 * @param userId     The UUID of the user who must own the playlist.
	 * @throws DAOException if the playlist is not found
	 *                      ({@link DAOErrorType#NOT_FOUND}), the user is not
	 *                      authorized to delete it
	 *                      ({@link DAOErrorType#ACCESS_DENIED}), or another
	 *                      database error occurs
	 *                      ({@link DAOErrorType#GENERIC_ERROR}).
	 */
	public void deletePlaylist(int playlistId, UUID userId) throws DAOException {
		logger.debug("Attempting to delete playlist ID: {} by user ID: {}", playlistId, userId);
		String deleteQuery = "DELETE FROM playlist_metadata WHERE idPlaylist = ? AND idUser = UUID_TO_BIN(?)";
		int affectedRows = 0;

		try (PreparedStatement pStatement = connection.prepareStatement(deleteQuery)) {
			pStatement.setInt(1, playlistId);
			pStatement.setString(2, userId.toString());
			affectedRows = pStatement.executeUpdate();

			if (affectedRows == 0) {
				// Could be NOT_FOUND or ACCESS_DENIED. Check if playlist exists first for
				// better error.
				String checkExistenceQuery = "SELECT 1 FROM playlist_metadata WHERE idPlaylist = ?";
				try (PreparedStatement checkStmt = connection.prepareStatement(checkExistenceQuery)) {
					checkStmt.setInt(1, playlistId);
					try (ResultSet rs = checkStmt.executeQuery()) {
						if (rs.next()) {
							// Playlist exists, so it must be an authorization issue
							logger.warn("Delete failed for playlist ID {}: User {} not authorized.", playlistId,
									userId);
							throw new DAOException("User not authorized to delete playlist ID " + playlistId,
									DAOErrorType.ACCESS_DENIED);
						} else {
							// Playlist doesn't exist
							logger.warn("Delete failed for playlist ID {}: Not found.", playlistId);
							throw new DAOException("Playlist with ID " + playlistId + " not found.",
									DAOErrorType.NOT_FOUND);
						}
					}
				} catch (SQLException checkEx) {
					logger.error("SQL error checking playlist existence during delete failure for ID {}: {}",
							playlistId, checkEx.getMessage(), checkEx);
					// Fallback to generic error if the check fails
					throw new DAOException(
							"Delete failed for playlist ID " + playlistId + ": Not found or user not authorized.",
							DAOErrorType.GENERIC_ERROR);
				}
			} else {
				logger.info("Playlist ID {} deleted successfully by user {}", playlistId, userId);
				// Success, no return value needed
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
	 * @throws DAOException if the playlist is not found
	 *                      ({@link DAOErrorType#NOT_FOUND}), the user is not
	 *                      authorized for the playlist
	 *                      ({@link DAOErrorType#ACCESS_DENIED}), the song is not
	 *                      found ({@link DAOErrorType#NOT_FOUND}), the song is
	 *                      already in the playlist
	 *                      ({@link DAOErrorType#DUPLICATE_ENTRY}), a constraint
	 *                      violation occurs
	 *                      ({@link DAOErrorType#CONSTRAINT_VIOLATION}), or another
	 *                      database error occurs
	 *                      ({@link DAOErrorType#GENERIC_ERROR}).
	 */
	public void addSongToPlaylist(int playlistId, UUID userId, int songId) throws DAOException {
		logger.debug("Attempting to add song ID: {} to playlist ID: {} by user ID: {}", songId, playlistId, userId);
		String checkPlaylistOwnershipQuery = "SELECT 1 FROM playlist_metadata WHERE idPlaylist = ? AND idUser = UUID_TO_BIN(?)";
		String checkSongExistenceQuery = "SELECT 1 FROM Song WHERE idSong = ?"; // Assuming Song table exists
		String insertQuery = "INSERT INTO playlist_content (idPlaylist, idSong) VALUES (?, ?)";

		// 1. Check Playlist Ownership
		try (PreparedStatement pStatement = connection.prepareStatement(checkPlaylistOwnershipQuery)) {
			pStatement.setInt(1, playlistId);
			pStatement.setString(2, userId.toString());
			try (ResultSet rs = pStatement.executeQuery()) {
				if (!rs.next()) {
					// Playlist not found or doesn't belong to user. Check if playlist exists at all
					// for better error.
					String checkExistenceQuery = "SELECT 1 FROM playlist_metadata WHERE idPlaylist = ?";
					try (PreparedStatement checkStmt = connection.prepareStatement(checkExistenceQuery)) {
						checkStmt.setInt(1, playlistId);
						try (ResultSet rsCheck = checkStmt.executeQuery()) {
							if (rsCheck.next()) {
								logger.warn("Add song failed: User {} not authorized for playlist ID {}", userId,
										playlistId);
								throw new DAOException("User not authorized for playlist ID " + playlistId,
										DAOErrorType.ACCESS_DENIED);
							} else {
								logger.warn("Add song failed: Playlist ID {} not found.", playlistId);
								throw new DAOException("Playlist with ID " + playlistId + " not found.",
										DAOErrorType.NOT_FOUND);
							}
						}
					} catch (SQLException checkEx) {
						logger.error("SQL error checking playlist existence during add song failure for ID {}: {}",
								playlistId, checkEx.getMessage(), checkEx);
						throw new DAOException("Add song failed: Playlist not found or user not authorized.",
								DAOErrorType.GENERIC_ERROR);
					}
				}
				// Ownership confirmed if we reach here
			}
		} catch (SQLException e) {
			logger.error("SQL error checking ownership before adding song {} to playlist {}: {}", songId, playlistId,
					e.getMessage(), e);
			throw new DAOException("Database error checking playlist ownership.", e, DAOErrorType.GENERIC_ERROR);
		}

		// 2. Check Song Existence
		try (PreparedStatement pStatement = connection.prepareStatement(checkSongExistenceQuery)) {
			pStatement.setInt(1, songId);
			try (ResultSet rs = pStatement.executeQuery()) {
				if (!rs.next()) {
					logger.warn("Add song failed: Song ID {} does not exist.", songId);
					throw new DAOException("Song with ID " + songId + " not found.", DAOErrorType.NOT_FOUND);
				}
			}
		} catch (SQLException e) {
			logger.error("SQL error checking existence for song ID {}: {}", songId, e.getMessage(), e);
			throw new DAOException("Database error checking song existence.", e, DAOErrorType.GENERIC_ERROR);
		}

		// 3. Insert song into playlist
		try (PreparedStatement pStatement = connection.prepareStatement(insertQuery)) {
			pStatement.setInt(1, playlistId);
			pStatement.setInt(2, songId);
			pStatement.executeUpdate();
			logger.info("Song ID {} added successfully to playlist ID {} by user {}", songId, playlistId, userId);
			// Success, no return value needed
		} catch (SQLException e) {
			if ("23000".equals(e.getSQLState())) { // Integrity constraint violation
				if (e.getMessage().toLowerCase().contains("duplicate entry") && e.getMessage().contains("PRIMARY")) { // DUPLICATE_ENTRY
					logger.warn("Attempt to add duplicate song ID {} to playlist ID {} (PK violation). Details: {}",
							songId, playlistId, e.getMessage());
					throw new DAOException("Song ID " + songId + " is already in playlist ID " + playlistId, e,
							DAOErrorType.DUPLICATE_ENTRY);
				} else if (e.getMessage().contains("fk_playlist-content_1")) { // NOT_FOUND (song doesn't exist)
					logger.warn("Attempt to add non-existent song ID {} to playlist ID {} (FK violation). Details: {}",
							songId, playlistId, e.getMessage());
					throw new DAOException("Song ID " + songId + " does not exist (FK check).", e,
							DAOErrorType.NOT_FOUND);
				} else { // Other CONSTRAINT_VIOLATION
					logger.error(
							"SQL integrity constraint violation adding song {} to playlist {}: SQLState={}, Message={}",
							songId, playlistId, e.getSQLState(), e.getMessage(), e);
					throw new DAOException("Database constraint violation adding song to playlist.", e,
							DAOErrorType.CONSTRAINT_VIOLATION);
				}
			} else { // Other SQL errors (GENERIC_ERROR - unexpected)
				logger.error("SQL error adding song {} to playlist {}: SQLState={}, Message={}", songId, playlistId,
						e.getSQLState(), e.getMessage(), e);
				throw new DAOException("Database error adding song to playlist.", e, DAOErrorType.GENERIC_ERROR);
			}
		}
	}

	/**
	 * Removes a song from a specific playlist owned by a user.
	 *
	 * @param playlistId The ID of the playlist to remove the song from.
	 * @param userId     The UUID of the user who must own the playlist.
	 * @param songId     The ID of the song to remove.
	 * @return true if the song was present and removed successfully, false if the
	 *         song was not in the playlist.
	 * @throws DAOException if the playlist is not found
	 *                      ({@link DAOErrorType#NOT_FOUND}), the user is not
	 *                      authorized for the playlist
	 *                      ({@link DAOErrorType#ACCESS_DENIED}), or another
	 *                      database error occurs
	 *                      ({@link DAOErrorType#GENERIC_ERROR}).
	 */
	public boolean removeSongFromPlaylist(int playlistId, UUID userId, int songId) throws DAOException {
		logger.debug("Attempting to remove song ID: {} from playlist ID: {} by user ID: {}", songId, playlistId,
				userId);
		String checkPlaylistOwnershipQuery = "SELECT 1 FROM playlist_metadata WHERE idPlaylist = ? AND idUser = UUID_TO_BIN(?)";
		String deleteQuery = "DELETE FROM playlist_content WHERE idPlaylist = ? AND idSong = ?";
		int affectedRows = 0;

		// 1. Check Playlist Ownership
		try (PreparedStatement pStatement = connection.prepareStatement(checkPlaylistOwnershipQuery)) {
			pStatement.setInt(1, playlistId);
			pStatement.setString(2, userId.toString());
			try (ResultSet rs = pStatement.executeQuery()) {
				if (!rs.next()) {
					// Playlist not found or doesn't belong to user. Check if playlist exists at all
					// for better error.
					String checkExistenceQuery = "SELECT 1 FROM playlist_metadata WHERE idPlaylist = ?";
					try (PreparedStatement checkStmt = connection.prepareStatement(checkExistenceQuery)) {
						checkStmt.setInt(1, playlistId);
						try (ResultSet rsCheck = checkStmt.executeQuery()) {
							if (rsCheck.next()) {
								logger.warn("Remove song failed: User {} not authorized for playlist ID {}", userId,
										playlistId);
								throw new DAOException("User not authorized for playlist ID " + playlistId,
										DAOErrorType.ACCESS_DENIED);
							} else {
								logger.warn("Remove song failed: Playlist ID {} not found.", playlistId);
								throw new DAOException("Playlist with ID " + playlistId + " not found.",
										DAOErrorType.NOT_FOUND);
							}
						}
					} catch (SQLException checkEx) {
						logger.error("SQL error checking playlist existence during remove song failure for ID {}: {}",
								playlistId, checkEx.getMessage(), checkEx);
						throw new DAOException("Remove song failed: Playlist not found or user not authorized.",
								DAOErrorType.GENERIC_ERROR);
					}
				}
				// Ownership confirmed if we reach here
			}
		} catch (SQLException e) {
			logger.error("SQL error checking ownership before removing song {} from playlist {}: {}", songId,
					playlistId, e.getMessage(), e);
			throw new DAOException("Database error checking ownership", e, DAOErrorType.GENERIC_ERROR);
		}

		// 2. Delete Song from Playlist
		try (PreparedStatement pStatement = connection.prepareStatement(deleteQuery)) {
			pStatement.setInt(1, playlistId);
			pStatement.setInt(2, songId);
			affectedRows = pStatement.executeUpdate();
			if (affectedRows > 0) {
				logger.info("Song ID {} removed successfully from playlist ID {} by user {}", songId, playlistId,
						userId);
			} else {
				// Song wasn't in the playlist, which is not an error state for removal.
				logger.debug("Song ID {} was not found in playlist ID {} for removal.", songId, playlistId);
			}
		} catch (SQLException e) {
			logger.error("SQL error removing song {} from playlist {}: {}", songId, playlistId, e.getMessage(), e);
			throw new DAOException("Database error removing song from playlist.", e, DAOErrorType.GENERIC_ERROR);
		}
		return affectedRows > 0; // Return true if removed, false if not present
	}

}
