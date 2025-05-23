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
	private static final String CHECK_PLAYLIST_EXISTS_BY_ID_QUERY = "SELECT 1 FROM playlist_metadata WHERE idPlaylist = ?";

	// Constants for createPlaylist
	private static final String CHECK_PLAYLIST_NAME_EXISTS_FOR_USER_QUERY = "SELECT idPlaylist FROM playlist_metadata WHERE name = ? AND idUser = UUID_TO_BIN(?)";
	private static final String CHECK_SONG_EXISTS_AND_BELONGS_TO_USER_QUERY = "SELECT 1 FROM Song WHERE idSong = ? AND idUser = UUID_TO_BIN(?)";
	private static final String INSERT_PLAYLIST_METADATA_QUERY = "INSERT INTO playlist_metadata (name, idUser) VALUES (?, UUID_TO_BIN(?))";
	private static final String INSERT_PLAYLIST_CONTENT_QUERY = "INSERT INTO playlist_content (idPlaylist, idSong) VALUES (?, ?)";

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
	public Playlist createPlaylist(String name, UUID idUser, List<Integer> songIds) throws DAOException {
		logger.debug("Attempting to create playlist: name={}, userId={}, songCount={}", name, idUser,
				songIds != null ? songIds.size() : 0);

		int newPlaylistId = -1;
		boolean previousAutoCommit = false;

		try {
			previousAutoCommit = connection.getAutoCommit();
			connection.setAutoCommit(false);

			checkPlaylistNameAvailability(name, idUser);

			verifyProvidedSongIds(songIds, idUser);

			newPlaylistId = insertNewPlaylistMetadata(name, idUser);

			addSongsToNewPlaylist(newPlaylistId, songIds);

			connection.commit();
			logger.info("Playlist ID {} created successfully for user {}", newPlaylistId, idUser);

		} catch (DAOException e) {
			logger.warn("Playlist creation failed for name={}, userId={} due to validation error: {}", name, idUser,
					e.getMessage());
			if (connection != null) {
				try {
					connection.rollback();
					logger.debug("Transaction rolled back due to DAOException during playlist creation.");
				} catch (SQLException ex) {
					logger.error("Rollback failed during DAOException handling: {}", ex.getMessage(), ex);
				}
			}
			throw e;
		} catch (SQLException e) {
			logger.warn("Transaction rolled back for playlist creation (name={}, userId={}) due to SQL error: {}", name,
					idUser, e.getMessage());
			if (connection != null) {
				try {
					connection.rollback();
				} catch (SQLException ex) {
					logger.error("Rollback failed during SQLException handling: {}", ex.getMessage(), ex);
				}
			}

			// Translate SQLException to specific DAOException
			throw translateCreatePlaylistSQLException(e, name, idUser, newPlaylistId);

		} finally {
			if (connection != null) {
				try {
					connection.setAutoCommit(previousAutoCommit);
				} catch (SQLException e) {
					logger.error("Failed to restore auto-commit state after playlist creation attempt: {}",
							e.getMessage(), e);
				}
			}
		}
		return this.findPlaylistById(newPlaylistId, idUser);
	}

	// --- Helper methods for createPlaylist ---

	private void checkPlaylistNameAvailability(String name, UUID idUser) throws DAOException, SQLException {
		logger.debug("Checking playlist name availability: name={}, userId={}", name, idUser);
		try (PreparedStatement pStatementCheck = connection
				.prepareStatement(CHECK_PLAYLIST_NAME_EXISTS_FOR_USER_QUERY)) {
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
			throw e;
		}
		logger.debug("Playlist name '{}' is available for user {}", name, idUser);
	}

	private void verifyProvidedSongIds(List<Integer> songIds, UUID idUser) throws DAOException, SQLException {
		if (songIds == null || songIds.isEmpty()) {
			logger.debug("No song IDs provided for verification.");
			return;
		}
		logger.debug("Verifying {} provided song IDs for user {}.", songIds.size(), idUser);
		for (Integer songId : songIds) {
			if (songId == null) {
				logger.warn("Playlist creation failed: Null song ID provided for user {}.", idUser);
				throw new DAOException("Playlist cannot contain null song IDs.", DAOErrorType.CONSTRAINT_VIOLATION);
			}

			checkSongExistsAndOwnership(songId, idUser);
		}
		logger.debug("All {} provided song IDs verified successfully for user {}.", songIds.size(), idUser);
	}

	private int insertNewPlaylistMetadata(String name, UUID idUser) throws SQLException {
		logger.debug("Inserting playlist metadata: name={}, userId={}", name, idUser);
		int newPlaylistId = -1;
		try (PreparedStatement pStatementMetadata = connection.prepareStatement(INSERT_PLAYLIST_METADATA_QUERY,
				Statement.RETURN_GENERATED_KEYS)) {
			pStatementMetadata.setString(1, name);
			pStatementMetadata.setString(2, idUser.toString());

			int affectedRows = pStatementMetadata.executeUpdate();

			if (affectedRows == 0) {
				logger.error("Creating playlist metadata failed, no rows affected for name={}, userId={}", name,
						idUser);
				throw new SQLException("Creating playlist metadata failed, no rows affected.");
			}
			try (ResultSet generatedKeys = pStatementMetadata.getGeneratedKeys()) {
				if (generatedKeys.next()) {
					newPlaylistId = generatedKeys.getInt(1);
					logger.debug("Playlist metadata created with ID: {}", newPlaylistId);
				} else {
					logger.error("Creating playlist metadata failed, no ID obtained for name={}, userId={}", name,
							idUser);
					throw new SQLException("Creating playlist metadata failed, no ID obtained.");
				}
			}
		} catch (SQLException e) {
			logger.error("SQL error inserting playlist metadata for name={}, userId={}: {}", name, idUser,
					e.getMessage(), e);
			throw e;
		}
		return newPlaylistId;
	}

	private void addSongsToNewPlaylist(int newPlaylistId, List<Integer> songIds) throws SQLException {
		if (songIds == null || songIds.isEmpty()) {
			logger.debug("No songs to add to playlist ID: {}", newPlaylistId);
			return;
		}
		logger.debug("Adding {} songs to playlist ID: {}", songIds.size(), newPlaylistId);
		try (PreparedStatement pStatementContent = connection.prepareStatement(INSERT_PLAYLIST_CONTENT_QUERY)) {
			for (Integer songId : songIds) {
				pStatementContent.setInt(1, newPlaylistId);
				pStatementContent.setInt(2, songId);
				pStatementContent.addBatch();
			}
			pStatementContent.executeBatch();
		} catch (SQLException e) {
			logger.error("SQL error adding songs to playlist ID {}: {}", newPlaylistId, e.getMessage(), e);
			throw e;
		}
		logger.debug("Successfully added {} songs to playlist ID: {}", songIds.size(), newPlaylistId);
	}

	private DAOException translateCreatePlaylistSQLException(SQLException e, String name, UUID idUser,
			int newPlaylistId) {
		String errorMessage = e.getMessage().toLowerCase();

		if ("23000".equals(e.getSQLState())) { // Integrity constraint violation
			if (errorMessage.contains("unique_playlist_per_user")) {
				logger.warn(
						"Playlist creation failed due to unique_playlist_per_user constraint: name={}, userId={}. Details: {}",
						name, idUser, e.getMessage());
				return new DAOException("Playlist name '" + name + "' already exists for this user.", e,
						DAOErrorType.NAME_ALREADY_EXISTS);
			}

			if (errorMessage.contains("unique_playlist_and_song")
					|| (errorMessage.contains("playlist_content") && errorMessage.contains("primary"))) {
				logger.warn(
						"Playlist creation failed due to unique_playlist_and_song constraint or PK violation on playlist_content: name={}, userId={}. Details: {}",
						name, idUser, e.getMessage());
				return new DAOException("Duplicate song ID found in the input list for the playlist.", e,
						DAOErrorType.DUPLICATE_ENTRY);
			}

			if (errorMessage.contains("fk_playlist-content_1")) {
				logger.error(
						"Playlist creation failed due to fk_playlist-content_1 (Song not found): playlistId={}, name={}, userId={}. Details: {}",
						newPlaylistId, name, idUser, e.getMessage(), e);
				return new DAOException("Error associating songs with playlist: A referenced song ID does not exist.",
						e, DAOErrorType.NOT_FOUND);
			}

			if (errorMessage.contains("fk_playlist-content_2")) {
				logger.error(
						"Playlist creation failed due to fk_playlist-content_2 (Playlist metadata not found): playlistId={}, name={}, userId={}. Details: {}",
						newPlaylistId, name, idUser, e.getMessage(), e);
				return new DAOException("Error associating songs with playlist: Playlist metadata inconsistency.", e,
						DAOErrorType.NOT_FOUND);
			}

			logger.error(
					"SQL integrity constraint violation during playlist creation for name={}, userId={}: SQLState={}, Message={}",
					name, idUser, e.getSQLState(), e.getMessage(), e);
			return new DAOException("Database integrity constraint violation during playlist creation.", e,
					DAOErrorType.CONSTRAINT_VIOLATION);
		}

		logger.error("SQL error during playlist creation transaction for name={}, userId={}: SQLState={}, Message={}",
				name, idUser, e.getSQLState(), e.getMessage(), e);
		return new DAOException("Database error during playlist creation.", e, DAOErrorType.GENERIC_ERROR);
	}

	/**
	 * Verifies if a playlist exists and if the specified user is authorized to
	 * access it. Throws DAOException if not found or access is denied.
	 */
	private void verifyPlaylistAccessible(int playlistId, UUID userId) throws DAOException, SQLException {
		logger.debug("Verifying access for playlist ID: {} by user ID: {}", playlistId, userId);
		String query = "SELECT BIN_TO_UUID(idUser) as ownerUUID FROM playlist_metadata WHERE idPlaylist = ?";
		UUID ownerUUID = null;

		try (PreparedStatement pStatement = connection.prepareStatement(query)) {
			pStatement.setInt(1, playlistId);
			try (ResultSet rs = pStatement.executeQuery()) {
				if (rs.next()) {
					String ownerUUIDString = rs.getString("ownerUUID");
					if (ownerUUIDString == null) {
						logger.error("Playlist ID {} has a null owner in the database.", playlistId);
						throw new DAOException("Playlist " + playlistId + " has inconsistent ownership data.",
								DAOErrorType.GENERIC_ERROR);
					}
					ownerUUID = UUID.fromString(ownerUUIDString);
				} else {
					logger.warn("Access check failed: Playlist ID {} not found.", playlistId);
					throw new DAOException("Playlist with ID " + playlistId + " not found.", DAOErrorType.NOT_FOUND);
				}
			}
		} catch (IllegalArgumentException e) {
			logger.error("Error parsing owner UUID for playlist ID {}: {}", playlistId, e.getMessage(), e);
			throw new DAOException("Error verifying playlist ownership due to invalid owner ID format.", e,
					DAOErrorType.GENERIC_ERROR);
		}

		if (!userId.equals(ownerUUID)) {
			logger.warn("Access check failed: User {} not authorized for playlist ID {} (owned by {}).", userId,
					playlistId, ownerUUID);
			throw new DAOException("User not authorized to access playlist ID " + playlistId + ".",
					DAOErrorType.ACCESS_DENIED);
		}
		logger.debug("Access verified for playlist ID: {} by user ID: {}", playlistId, userId);
	}

	/**
	 * Checks if a song with the given ID exists and is owned by the specified user.
	 * Throws DAOException if not found or not owned.
	 */
	private void checkSongExistsAndOwnership(int songId, UUID idUser) throws DAOException, SQLException {
		logger.debug("Checking existence and ownership for song ID: {} by user ID: {}", songId, idUser);
		try (PreparedStatement pStatement = connection.prepareStatement(CHECK_SONG_EXISTS_AND_BELONGS_TO_USER_QUERY)) {
			pStatement.setInt(1, songId);
			pStatement.setString(2, idUser.toString());
			try (ResultSet rs = pStatement.executeQuery()) {
				if (!rs.next()) {
					logger.warn("Song existence/ownership check failed: Song ID {} not found or not owned by user {}.",
							songId, idUser);
					throw new DAOException("Song with ID " + songId + " not found or not accessible to this user.",
							DAOErrorType.NOT_FOUND);
				}
			}
		} catch (SQLException e) {
			logger.error("SQL error checking song existence/ownership for songID {}, userID {}: {}", songId, idUser,
					e.getMessage(), e);
			throw e; // Re-throw
		}
		logger.debug("Song ID {} verified to exist and belong to user {}.", songId, idUser);
	}

	/**
	 * Translates SQLExceptions occurring during addSongToPlaylist operations.
	 */
	private DAOException translateAddSongToPlaylistSQLException(SQLException e, int playlistId, UUID userId,
			int songId) {
		String errorMessage = e.getMessage().toLowerCase();
		if ("23000".equals(e.getSQLState())) {
			if (errorMessage.contains("unique_playlist_and_song")
					|| (errorMessage.contains("playlist_content") && errorMessage.contains("primary"))
					|| (errorMessage.contains("duplicate entry") && errorMessage.contains("primary"))) {
				logger.warn("Attempt to add duplicate song ID {} to playlist ID {} by user {}. Details: {}", songId,
						playlistId, userId, e.getMessage());
				return new DAOException("Song ID " + songId + " is already in playlist ID " + playlistId + ".", e,
						DAOErrorType.DUPLICATE_ENTRY);
			} else if (errorMessage.contains("fk_playlist-content_1")) {
				logger.warn(
						"Attempt to add non-existent song ID {} to playlist ID {} by user {} (FK violation on song). Details: {}",
						songId, playlistId, userId, e.getMessage());
				return new DAOException("Song with ID " + songId + " could not be added because it does not exist.", e,
						DAOErrorType.NOT_FOUND);
			} else if (errorMessage.contains("fk_playlist-content_2")) {
				logger.warn(
						"Attempt to add song ID {} to non-existent playlist ID {} by user {} (FK violation on playlist). Details: {}",
						songId, playlistId, userId, e.getMessage());
				return new DAOException("Playlist with ID " + playlistId + " could not be found to add song.", e,
						DAOErrorType.NOT_FOUND);
			} else {
				logger.error(
						"SQL integrity constraint violation adding song {} to playlist {} by user {}: SQLState={}, Message={}",
						songId, playlistId, userId, e.getSQLState(), e.getMessage(), e);
				return new DAOException("Database constraint violation adding song to playlist.", e,
						DAOErrorType.CONSTRAINT_VIOLATION);
			}
		} else {
			logger.error("SQL error adding song {} to playlist {} by user {}: SQLState={}, Message={}", songId,
					playlistId, userId, e.getSQLState(), e.getMessage(), e);
			return new DAOException("Database error adding song to playlist.", e, DAOErrorType.GENERIC_ERROR);
		}
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
		String queryMetadata = "SELECT name, birthday, BIN_TO_UUID(idUser) as userUUID FROM playlist_metadata WHERE idPlaylist = ?";
		String queryContent = "SELECT idSong FROM playlist_content WHERE idPlaylist = ?";

		try {
			verifyPlaylistAccessible(playlistId, userId);

			// Fetch Full Playlist Details
			try (PreparedStatement pStatementMetadata = connection.prepareStatement(queryMetadata)) {
				pStatementMetadata.setInt(1, playlistId);
				try (ResultSet rsMetadata = pStatementMetadata.executeQuery()) {
					if (rsMetadata.next()) {
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
									DAOErrorType.GENERIC_ERROR);
						}
						playlist.setSongs(songIds);
						logger.debug("Successfully retrieved playlist ID: {} owned by user ID: {}", playlistId, userId);
					} else {
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
		} catch (SQLException e) {
			logger.error("SQL error during findPlaylistById for playlistID {}: {}", playlistId, e.getMessage(), e);
			throw new DAOException("Database error while finding playlist by ID.", e, DAOErrorType.GENERIC_ERROR);
		} catch (IllegalArgumentException e) {
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

		try {
			verifyPlaylistAccessible(playlistId, userId);

			try (PreparedStatement pStatement = connection.prepareStatement(deleteQuery)) {
				pStatement.setInt(1, playlistId);
				pStatement.setString(2, userId.toString());
				affectedRows = pStatement.executeUpdate();

				if (affectedRows == 0) {
					logger.warn(
							"Delete operation affected 0 rows for playlist ID {} by user {}, though access was verified. This might indicate a concurrent modification or an unexpected issue.",
							playlistId, userId);

					try (PreparedStatement checkStmt = connection.prepareStatement(CHECK_PLAYLIST_EXISTS_BY_ID_QUERY)) {
						checkStmt.setInt(1, playlistId);
						try (ResultSet rs = checkStmt.executeQuery()) {
							if (!rs.next()) {
								throw new DAOException(
										"Playlist with ID " + playlistId
												+ " was not found for deletion (possibly deleted concurrently).",
										DAOErrorType.NOT_FOUND);
							} else {
								throw new DAOException(
										"Failed to delete playlist ID " + playlistId
												+ " despite verified access. Unknown reason.",
										DAOErrorType.GENERIC_ERROR);
							}
						}
					}
				} else {
					logger.info("Playlist ID {} deleted successfully by user {}", playlistId, userId);
				}
			}
		} catch (SQLException e) {
			logger.error("SQL error deleting playlist ID {} by user {}: {}", playlistId, userId, e.getMessage(), e);
			throw new DAOException("Database error deleting playlist ID " + playlistId + ".", e,
					DAOErrorType.GENERIC_ERROR);
		} catch (DAOException e) {
			logger.warn("Pre-delete check failed for playlist ID {} by user {}: {}", playlistId, userId,
					e.getMessage());
			throw e;
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

		try {
			verifyPlaylistAccessible(playlistId, userId);

			checkSongExistsAndOwnership(songId, userId);

			// Insert song into playlist
			try (PreparedStatement pStatement = connection.prepareStatement(INSERT_PLAYLIST_CONTENT_QUERY)) {
				pStatement.setInt(1, playlistId);
				pStatement.setInt(2, songId);
				pStatement.executeUpdate();
				logger.info("Song ID {} added successfully to playlist ID {} by user {}", songId, playlistId, userId);
			}
		} catch (SQLException e) {
			if (e.getSQLState() != null && e.getSQLState().startsWith("23")) {
				throw translateAddSongToPlaylistSQLException(e, playlistId, userId, songId);
			} else {
				logger.error("SQL error during addSongToPlaylist for playlist {}, song {}, user {}: {}", playlistId,
						songId, userId, e.getMessage(), e);
				throw new DAOException("Database error while attempting to add song to playlist.", e,
						DAOErrorType.GENERIC_ERROR);
			}
		} catch (DAOException e) {
			logger.warn("Pre-check failed for adding song {} to playlist {} by user {}: {}", songId, playlistId, userId,
					e.getMessage());
			throw e;
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
		String deleteQuery = "DELETE FROM playlist_content WHERE idPlaylist = ? AND idSong = ?";
		int affectedRows = 0;

		try {
			verifyPlaylistAccessible(playlistId, userId);

			// Delete Song from Playlist
			try (PreparedStatement pStatement = connection.prepareStatement(deleteQuery)) {
				pStatement.setInt(1, playlistId);
				pStatement.setInt(2, songId);
				affectedRows = pStatement.executeUpdate();
				if (affectedRows > 0) {
					logger.info("Song ID {} removed successfully from playlist ID {} by user {}", songId, playlistId,
							userId);
				} else {
					logger.debug("Song ID {} was not found in playlist ID {} for removal, or was already removed.",
							songId, playlistId);
				}
			}
		} catch (SQLException e) {
			logger.error("SQL error removing song {} from playlist {} by user {}: {}", songId, playlistId, userId,
					e.getMessage(), e);
			throw new DAOException("Database error while attempting to remove song from playlist.", e,
					DAOErrorType.GENERIC_ERROR);
		} catch (DAOException e) {
			logger.warn("Pre-check failed for removing song {} from playlist {} by user {}: {}", songId, playlistId,
					userId, e.getMessage());
			throw e;
		}
		return affectedRows > 0;
	}

}
