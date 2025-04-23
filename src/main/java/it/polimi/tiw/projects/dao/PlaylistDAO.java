package it.polimi.tiw.projects.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import it.polimi.tiw.projects.beans.Playlist;
import it.polimi.tiw.projects.exceptions.DAOException;
import it.polimi.tiw.projects.exceptions.DAOException.DAOErrorType;

public class PlaylistDAO {
	private Connection connection;

	/**
	 * Constructs a new PlaylistDAO with the given database connection.
	 *
	 * @param connection The database connection to use.
	 */
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
						throw new DAOException("Playlist name '" + name + "' already exists for this user.",
								DAOErrorType.NAME_ALREADY_EXISTS);
					}
				}
			}

			// Insert playlist metadata
			try (PreparedStatement pStatementMetadata = connection.prepareStatement(insertMetadataSQL,
					Statement.RETURN_GENERATED_KEYS)) {
				pStatementMetadata.setString(1, name);
				pStatementMetadata.setString(2, image);
				pStatementMetadata.setString(3, idUser.toString());

				int affectedRows = pStatementMetadata.executeUpdate();

				if (affectedRows == 0) {
					connection.rollback(); // Rollback before throwing
					throw new SQLException("Creating playlist metadata failed, no rows affected.");
				}
				try (ResultSet generatedKeys = pStatementMetadata.getGeneratedKeys()) {
					if (generatedKeys.next()) {
						newPlaylistId = generatedKeys.getInt(1);
					} else {
						connection.rollback(); // Rollback before throwing
						throw new SQLException("Creating playlist metadata failed, no ID obtained.");
					}
				}
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
				}
			}

			connection.commit(); // Commit transaction

		} catch (SQLException e) {
			try {
				connection.rollback(); // Rollback transaction on error
			} catch (SQLException ex) {
				System.err.println("Rollback failed: " + ex.getMessage());
			}

			// Handle specific constraint violations with DAOException
			if ("23000".equals(e.getSQLState())) { // Integrity constraint violation
				if (e.getMessage().contains("unique_playlist_per_user")) {
					throw new DAOException("Playlist name '" + name + "' already exists for this user.", e,
							DAOErrorType.NAME_ALREADY_EXISTS);
				} else if (e.getMessage().contains("unique_playlist_and_song")) {
					// Decide if this is NAME_ALREADY_EXISTS or GENERIC_ERROR. Let's use GENERIC for
					// now.
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
					System.err.println("Failed to restore auto-commit: " + e.getMessage());
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
		List<Integer> playlistIds = new ArrayList<>();
		String query = "SELECT idPlaylist FROM `playlist-metadata` WHERE idUser = UUID_TO_BIN(?)";

		try (PreparedStatement pStatement = connection.prepareStatement(query)) {
			pStatement.setString(1, idUser.toString());
			try (ResultSet resultSet = pStatement.executeQuery()) {
				while (resultSet.next()) {
					playlistIds.add(resultSet.getInt("idPlaylist"));
				}
			}
		} catch (SQLException e) {
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
					}
					playlist.setSongs(songIds);
				}
				// If rsMetadata.next() is false, the playlist doesn't exist or doesn't belong
				// to the user, return null
			}
		} catch (SQLException e) {
			throw new DAOException("Database error finding playlist by ID.", e, DAOErrorType.GENERIC_ERROR);
		} catch (IllegalArgumentException e) {
			// Catch potential UUID parsing errors
			throw new DAOException("Error parsing UUID from database.", e, DAOErrorType.GENERIC_ERROR);
		}
		return playlist;
	}

	/**
	 * Deletes a specific playlist owned by a user.
	 * Relies on the database's ON DELETE CASCADE constraint to remove associated
	 * songs from the `playlist-content` table.
	 *
	 * @param playlistId The ID of the playlist to delete.
	 * @param userId     The UUID of the user who must own the playlist.
	 * @return true if the playlist was found and deleted, false otherwise.
	 * @throws DAOException if a database access error occurs.
	 */
	public boolean deletePlaylist(int playlistId, UUID userId) throws DAOException {
		String deleteQuery = "DELETE FROM `playlist-metadata` WHERE idPlaylist = ? AND idUser = UUID_TO_BIN(?)";
		int affectedRows = 0;

		try (PreparedStatement pStatement = connection.prepareStatement(deleteQuery)) {
			pStatement.setInt(1, playlistId);
			pStatement.setString(2, userId.toString());
			affectedRows = pStatement.executeUpdate();

		} catch (SQLException e) {
			// This could indicate a problem with the DELETE statement itself or potentially
			// an issue during the cascade operation if constraints/permissions interfere.
			throw new DAOException("Database error deleting playlist metadata for ID: " + playlistId, e,
					DAOErrorType.GENERIC_ERROR);
		}
		return affectedRows > 0;
	}

	/**
	 * Adds a song to a specific playlist owned by a user.
	 * Checks if the playlist exists and belongs to the user before adding the song.
	 *
	 * @param playlistId The ID of the playlist to add the song to.
	 * @param userId     The UUID of the user who must own the playlist.
	 * @param songId     The ID of the song to add.
	 * @return true if the song was added successfully, false if the playlist was
	 *         not found or doesn't belong to the user.
	 * @throws DAOException if a database access error occurs.
	 */
	public boolean addSongToPlaylist(int playlistId, UUID userId, int songId) throws DAOException {
		String checkOwnershipQuery = "SELECT * FROM `playlist-metadata` WHERE idPlaylist = ? AND idUser = UUID_TO_BIN(?)";
		String insertQuery = "INSERT INTO `playlist-content` (idPlaylist, idSong) VALUES (?, ?)";
		int affectedRows = 0;

		// Check ownership
		try (PreparedStatement pStatement = connection.prepareStatement(checkOwnershipQuery)) {
			pStatement.setInt(1, playlistId);
			pStatement.setString(2, userId.toString());
			try (ResultSet rs = pStatement.executeQuery()) {
				if (!rs.next())
					return false; // Playlist not found or doesn't belong to user.
			}
		} catch (SQLException e) {
			throw new DAOException("Database error checking ownership.", e, DAOErrorType.GENERIC_ERROR);
		}

		// Insert song into playlist
		try (PreparedStatement pStatement = connection.prepareStatement(insertQuery)) {
			pStatement.setInt(1, playlistId);
			pStatement.setInt(2, songId);
			affectedRows = pStatement.executeUpdate();
		} catch (SQLException e) {
			throw new DAOException("Database error adding song to playlist.", e, DAOErrorType.GENERIC_ERROR);
		}
		return affectedRows > 0;
	}

	/**
	 * Removes a song from a specific playlist owned by a user.
	 * Checks if the playlist exists and belongs to the user before removing the
	 * song.
	 *
	 * @param playlistId The ID of the playlist to remove the song from.
	 * @param userId     The UUID of the user who must own the playlist.
	 * @param songId     The ID of the song to remove.
	 * @return true if the song was removed successfully, false if the playlist was
	 *         not found or doesn't belong to the user.
	 * @throws DAOException if a database access error occurs.
	 */
	public boolean removeSongFromPlaylist(int playlistId, UUID userId, int songId) throws DAOException {
		String checkOwnershipQuery = "SELECT * FROM `playlist-metadata` WHERE idPlaylist = ? AND idUser = UUID_TO_BIN(?)";
		String deleteQuery = "DELETE FROM `playlist-content` WHERE idPlaylist = ? AND idSong = ?";
		int affectedRows = 0;

		// Check ownership
		try (PreparedStatement pStatement = connection.prepareStatement(checkOwnershipQuery)) {
			pStatement.setInt(1, playlistId);
			pStatement.setString(2, userId.toString());
			try (ResultSet rs = pStatement.executeQuery()) {
				if (!rs.next())
					return false; // Playlist not found or doesn't belong to user.
			}

		} catch (SQLException e) {
			throw new DAOException("Database error checking ownership", e, DAOErrorType.GENERIC_ERROR);
		}

		// Delete Song from Playlist
		try (PreparedStatement pStatement = connection.prepareStatement(deleteQuery)) {
			pStatement.setInt(1, playlistId);
			pStatement.setInt(2, songId);
			affectedRows = pStatement.executeUpdate();
		} catch (SQLException e) {
			throw new DAOException("Database error removing song from playlist.", e, DAOErrorType.GENERIC_ERROR);
		}
		return affectedRows > 0;
	}

}
