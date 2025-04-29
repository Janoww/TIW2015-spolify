package it.polimi.tiw.projects.dao;

import it.polimi.tiw.projects.beans.Song;
import it.polimi.tiw.projects.exceptions.DAOException;
import it.polimi.tiw.projects.utils.SongRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class SongDAO {
	private static final Logger logger = LoggerFactory.getLogger(SongDAO.class);
	private Connection connection;

	public SongDAO(Connection connection) {
		this.connection = connection;
	}

	/**
	 * Creates a new song in the database.
	 *
	 * @param title     The title of the song.
	 * @param idAlbum   The ID of the album the song belongs to.
	 * @param year      The release year of the song.
	 * @param genre     The genre of the song.
	 * @param audioFile The path or URL to the audio file.
	 * @param idUser    The UUID of the user who uploaded the song.
	 * @return The newly created Song object with its generated ID.
	 * @throws DAOException if a database access error occurs.
	 */
	public Song createSong(String title, int idAlbum, int year, String genre, String audioFile, UUID idUser)
			throws DAOException {
		logger.debug("Attempting to create song: title={}, idAlbum={}, year={}, genre={}, audioFile={}, userId={}",
				title, idAlbum, year, genre, audioFile, idUser);
		String query = "INSERT into Song (title, idAlbum, year, genre, audioFile, idUser) VALUES(?, ?, ?, ?, ?, UUID_TO_BIN(?))";
		Song newSong = null;

		try (PreparedStatement pStatement = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
			pStatement.setString(1, title);
			pStatement.setInt(2, idAlbum);
			pStatement.setInt(3, year);
			pStatement.setString(4, genre);
			pStatement.setString(5, audioFile);
			pStatement.setString(6, idUser.toString());
			int affectedRows = pStatement.executeUpdate();

			if (affectedRows == 0) {
				// This case might not happen with auto-increment keys but kept for robustness
				throw new DAOException("Creating song failed, no rows affected.",
						DAOException.DAOErrorType.GENERIC_ERROR);
			}

			try (ResultSet generatedKeys = pStatement.getGeneratedKeys()) {
				if (generatedKeys.next()) {
					int newId = generatedKeys.getInt(1);
					// Create the Song bean
					newSong = new Song();
					newSong.setIdSong(newId);
					newSong.setTitle(title);
					newSong.setIdAlbum(idAlbum);
					newSong.setYear(year);
					newSong.setGenre(genre);
					newSong.setAudioFile(audioFile);
					newSong.setIdUser(idUser);
					logger.info("Song created successfully with ID: {}", newId);

					// Add to registry if initialized
					if (SongRegistry.isInitialized()) {
						SongRegistry.addSong(newSong);
						logger.debug("Song ID {} added to SongRegistry.", newId);
					} else {
						logger.warn("SongRegistry not initialized when creating song ID: {}", newId);
					}
				} else {
					logger.error("Creating song failed, no ID obtained for title={}, userId={}", title, idUser);
					throw new DAOException("Creating song failed, no ID obtained.",
							DAOException.DAOErrorType.GENERIC_ERROR);
				}
			}
		} catch (SQLException e) {
			logger.error("SQL error creating song title={}, userId={}: {}", title, idUser, e.getMessage(), e);
			throw new DAOException("Error creating song: " + e.getMessage(), e,
					DAOException.DAOErrorType.GENERIC_ERROR);
		}
		return newSong;
	}

	/**
	 * Finds all songs uploaded by a specific user.
	 *
	 * @param userId The UUID of the user.
	 * @return A list of songs uploaded by the user.
	 * @throws DAOException if a database access error occurs.
	 */
	public List<Song> findSongsByUser(UUID userId) throws DAOException {
		logger.debug("Attempting to find songs for user ID: {}", userId);
		List<Song> songs = new ArrayList<>();
		String query = "SELECT idSong, title, idAlbum, year, genre, audioFile, BIN_TO_UUID(idUser) as idUser FROM Song WHERE idUser = UUID_TO_BIN(?)";
		try (PreparedStatement pStatement = connection.prepareStatement(query)) {
			pStatement.setString(1, userId.toString());
			try (ResultSet result = pStatement.executeQuery()) {
				while (result.next()) {
					Song song = new Song();
					song.setIdSong(result.getInt("idSong"));
					song.setTitle(result.getString("title"));
					song.setIdAlbum(result.getInt("idAlbum"));
					song.setYear(result.getInt("year"));
					song.setGenre(result.getString("genre"));
					song.setAudioFile(result.getString("audioFile"));
					song.setIdUser(UUID.fromString(result.getString("idUser")));
					songs.add(song);
				}
				logger.debug("Found {} songs for user ID: {}", songs.size(), userId);
			}
		} catch (SQLException | IllegalArgumentException e) { // Catch UUID parsing errors too
			logger.error("Error finding songs for user ID {}: {}", userId, e.getMessage(), e);
			throw new DAOException("Error finding songs by user: " + e.getMessage(), e,
					DAOException.DAOErrorType.GENERIC_ERROR);
		}
		return songs;
	}

	/**
	 * Finds all songs in the database.
	 *
	 * @return A list of all songs.
	 * @throws DAOException if a database access error occurs.
	 */
	public List<Song> findAllSongs() throws DAOException {
		logger.debug("Attempting to find all songs");
		List<Song> songs = new ArrayList<>();
		String query = "SELECT idSong, title, idAlbum, year, genre, audioFile, BIN_TO_UUID(idUser) as idUser FROM Song";
		try (Statement statement = connection.createStatement(); ResultSet result = statement.executeQuery(query)) {
			while (result.next()) {
				Song song = new Song();
				song.setIdSong(result.getInt("idSong"));
				song.setTitle(result.getString("title"));
				song.setIdAlbum(result.getInt("idAlbum"));
				song.setYear(result.getInt("year"));
				song.setGenre(result.getString("genre"));
				song.setAudioFile(result.getString("audioFile"));
				song.setIdUser(UUID.fromString(result.getString("idUser")));
				songs.add(song);
			}
			logger.debug("Found {} songs in total.", songs.size());
		} catch (SQLException | IllegalArgumentException e) { // Catch UUID parsing errors too
			logger.error("Error finding all songs: {}", e.getMessage(), e);
			throw new DAOException("Error finding all songs: " + e.getMessage(), e,
					DAOException.DAOErrorType.GENERIC_ERROR);
		}
		return songs;
	}

	/**
	 * Initializes the SongRegistry with all songs from the database. Should be
	 * called once at application startup.
	 *
	 * @param connection A valid database connection.
	 * @throws DAOException          if a database access error occurs during song
	 *                               retrieval.
	 * @throws IllegalStateException if the registry is already initialized.
	 */
	public static synchronized void initializeRegistry(Connection connection) throws DAOException {
		logger.info("Attempting to initialize SongRegistry...");
		// Check initialization status using SongRegistry method
		if (SongRegistry.isInitialized()) {
			// System.out.println("SongRegistry already initialized. Skipping DB load.");
			logger.info("SongRegistry already initialized. Skipping DB load.");
			return; // Exit if already initialized
		}
		try {
			SongDAO dao = new SongDAO(connection); // Create a temporary DAO instance
			List<Song> allSongs = dao.findAllSongs(); // Fetch all songs
			SongRegistry.initialize(allSongs); // Initialize the registry
			logger.info("SongRegistry initialized successfully with {} songs.", allSongs.size());
		} catch (DAOException e) {
			logger.error("Failed to initialize SongRegistry due to DAOException: {}", e.getMessage(), e);
			throw e; // Re-throw the exception after logging
		} catch (Exception e) { // Catch any other potential runtime exceptions during initialization
			logger.error("Unexpected error during SongRegistry initialization: {}", e.getMessage(), e);
			throw new DAOException("Unexpected error initializing song registry.", e,
					DAOException.DAOErrorType.GENERIC_ERROR);
		}
	}

	/**
	 * Deletes a song from the database and removes it from the SongRegistry.
	 *
	 * @param songId The ID of the song to delete.
	 * @return true if the song was successfully deleted, false otherwise.
	 * @throws DAOException if a database access error occurs or the song is not
	 *                      found.
	 */
	public boolean deleteSong(int songId) throws DAOException {
		logger.debug("Attempting to delete song ID: {}", songId);
		// Remove from registry first (if initialized)
		// ? Note: If removeSong throws an exception (e.g., song not in registry),
		// ? it might prevent DB deletion. Consider if this is the desired behavior.
		boolean removedFromRegistry = false;
		if (SongRegistry.isInitialized()) {
			if (SongRegistry.removeSong(songId) != null) {
				logger.debug("Song ID {} removed from SongRegistry.", songId);
				removedFromRegistry = true;
			} else {
				logger.warn("Attempted to delete song ID {}, but it was not found in the initialized SongRegistry.",
						songId);
				// ? Decide if this should prevent DB deletion. Current logic allows DB delete
				// ? attempt.
				// return false; // Uncomment this line if not found in registry means deletion
				// should fail.
			}
		} else {
			// System.err.println("WARN: SongRegistry not initialized when deleting song ID:
			// " + songId);
			logger.warn("SongRegistry not initialized when deleting song ID: {}", songId);
		}

		String query = "DELETE FROM Song WHERE idSong = ?";
		try (PreparedStatement pStatement = connection.prepareStatement(query)) {
			pStatement.setInt(1, songId);
			int affectedRows = pStatement.executeUpdate();
			if (affectedRows == 0) {
				// If the song wasn't in the registry AND not in the DB, this is consistent.
				// ? If it WAS in the registry but not deleted from DB, that's an issue,
				// ? but this exception covers the "not found in DB" case.
				logger.warn("Deleting song ID {} from database failed (0 rows affected). Song might not exist in DB.",
						songId);
				// If it was removed from registry but not found in DB, maybe log inconsistency?
				if (removedFromRegistry) {
					logger.error(
							"Inconsistency: Song ID {} was removed from registry but not found in database for deletion.",
							songId);
					// Depending on requirements, maybe throw an exception here.
				}
				// Throwing NOT_FOUND is appropriate if the primary goal is DB deletion.
				throw new DAOException("Deleting song failed, song ID " + songId + " not found in database.",
						DAOException.DAOErrorType.NOT_FOUND);
			}
			logger.info("Song ID {} deleted successfully from database.", songId);
		} catch (SQLException e) {
			logger.error("SQL error deleting song ID {}: {}", songId, e.getMessage(), e);
			// ? Consider if the song should be re-added to the registry if DB deletion
			// ? fails after registry removal.
			// ? This depends on transactional requirements not implemented here.
			throw new DAOException("Error deleting song: " + e.getMessage(), e,
					DAOException.DAOErrorType.GENERIC_ERROR);
		}
		return true;
	}
}
