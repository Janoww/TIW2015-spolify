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

	public boolean deletePlaylist(Integer playlistId, UUID userId) {
		// TODO Auto-generated method stub
		String query = "SELECT * FROM `playlist-metadata` WHERE idPlaylist = ? AND idUser = UUID_TO_BIN(?)";
		String deleteQuery = "DELETE FROM `playlist-metadata` WHERE idPlaylist = ? AND idUser = UUID_TO_BIN(?)";
		throw new UnsupportedOperationException("Unimplemented method 'deletePlaylist'");

	}

	public boolean addSongToPlaylist(Integer playlistId, int createdSongId2) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Unimplemented method 'addSongToPlaylist'");
	}

	public boolean removeSongFromPlaylist(Integer playlistId, Integer createdSongId) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Unimplemented method 'removeSongFromPlaylist'");
	}

}
