package it.polimi.tiw.projects.dao;

import it.polimi.tiw.projects.beans.Song;
import it.polimi.tiw.projects.exceptions.DAOException;
import it.polimi.tiw.projects.utils.SongRegistry;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class SongDAO {
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
        String query = "INSERT into Song (title, idAlbum, year, genre, audioFile, idUser) VALUES(?, ?, ?, ?, ?, UUID_TO_BIN(?))";
        Song newSong = null;
        ResultSet generatedKeys = null;

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

            generatedKeys = pStatement.getGeneratedKeys();
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

                // Add to registry if initialized
                if (SongRegistry.isInitialized()) {
                    SongRegistry.addSong(newSong);
                } else {
                    System.err.println("WARN: SongRegistry not initialized when creating song ID: " + newId);
                }
            } else {
                throw new DAOException("Creating song failed, no ID obtained.",
                        DAOException.DAOErrorType.GENERIC_ERROR);
            }
        } catch (SQLException e) {
            throw new DAOException("Error creating song: " + e.getMessage(), e,
                    DAOException.DAOErrorType.GENERIC_ERROR);
        } finally {
            try {
                if (generatedKeys != null)
                    generatedKeys.close();
            } catch (SQLException e) {
                System.err.println("Failed to close ResultSet: " + e.getMessage());
                throw new DAOException("Failed to close resources during song creation", e,
                        DAOException.DAOErrorType.GENERIC_ERROR);
            }
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
        List<Song> songs = new ArrayList<>();
        String query = "SELECT idSong, title, idAlbum, year, genre, audioFile, BIN_TO_UUID(idUser) as idUser FROM Song WHERE idUser = UUID_TO_BIN(?)";
        ResultSet result = null; // Declare outside for finally block
        try (PreparedStatement pStatement = connection.prepareStatement(query)) {
            pStatement.setString(1, userId.toString());
            result = pStatement.executeQuery();
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
        } catch (SQLException e) {
            throw new DAOException("Error finding songs by user: " + e.getMessage(), e,
                    DAOException.DAOErrorType.GENERIC_ERROR);
        } finally {
            try {
                if (result != null)
                    result.close();
            } catch (SQLException e) {
                System.err.println("Failed to close ResultSet: " + e.getMessage());
                throw new DAOException("Failed to close resources finding songs by user", e,
                        DAOException.DAOErrorType.GENERIC_ERROR);
            }
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
        List<Song> songs = new ArrayList<>();
        String query = "SELECT idSong, title, idAlbum, year, genre, audioFile, BIN_TO_UUID(idUser) as idUser FROM Song";
        ResultSet result = null; // Declare outside for finally block
        try (Statement statement = connection.createStatement()) {
            result = statement.executeQuery(query);
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
        } catch (SQLException e) {
            throw new DAOException("Error finding all songs: " + e.getMessage(), e,
                    DAOException.DAOErrorType.GENERIC_ERROR);
        } finally {
            try {
                if (result != null)
                    result.close();
            } catch (SQLException e) {
                System.err.println("Failed to close ResultSet: " + e.getMessage());
                // Optionally rethrow
                throw new DAOException("Failed to close resources finding all songs", e,
                        DAOException.DAOErrorType.GENERIC_ERROR);
            }
        }
        return songs;
    }

    /**
     * Initializes the SongRegistry with all songs from the database.
     * Should be called once at application startup.
     *
     * @param connection A valid database connection.
     * @throws DAOException          if a database access error occurs during song
     *                               retrieval.
     * @throws IllegalStateException if the registry is already initialized.
     */
    public static synchronized void initializeRegistry(Connection connection) throws DAOException {
        // Check initialization status using SongRegistry method
        if (SongRegistry.isInitialized()) {
            System.out.println("SongRegistry already initialized. Skipping DB load.");
            return; // Exit if already initialized
        }
        SongDAO dao = new SongDAO(connection); // Create a temporary DAO instance
        List<Song> allSongs = dao.findAllSongs(); // Fetch all songs
        SongRegistry.initialize(allSongs); // Initialize the registry
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
        // Remove from registry first (if initialized)
        // Note: If removeSong throws an exception (e.g., song not in registry),
        // it might prevent DB deletion. Consider if this is the desired behavior.
        if (SongRegistry.isInitialized()) {
            if (SongRegistry.removeSong(songId) == null) {
                return false;
            }
        } else {
            System.err.println("WARN: SongRegistry not initialized when deleting song ID: " + songId);
        }

        String query = "DELETE FROM Song WHERE idSong = ?";
        try (PreparedStatement pStatement = connection.prepareStatement(query)) {
            pStatement.setInt(1, songId);
            int affectedRows = pStatement.executeUpdate();
            if (affectedRows == 0) {
                // If the song wasn't in the registry AND not in the DB, this is consistent.
                // If it WAS in the registry but not deleted from DB, that's an issue,
                // but this exception covers the "not found in DB" case.
                throw new DAOException("Deleting song failed, song ID " + songId + " not found.",
                        DAOException.DAOErrorType.NOT_FOUND);
            }
        } catch (SQLException e) {
            throw new DAOException("Error deleting song: " + e.getMessage(), e,
                    DAOException.DAOErrorType.GENERIC_ERROR);
        }
        return true;
    }
}
