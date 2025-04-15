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
     * @throws SQLException if a database access error occurs.
     */
    public Song createSong(String title, int idAlbum, int year, String genre, String audioFile, UUID idUser)
            throws SQLException {
        String query = "INSERT into Song (title, idAlbum, year, genre, audioFile, idUser) VALUES(?, ?, ?, ?, ?, UUID_TO_BIN(?))";
        Song newSong = null;
        ResultSet generatedKeys = null;

        // Use RETURN_GENERATED_KEYS to get the new song ID
        try (PreparedStatement pStatement = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            pStatement.setString(1, title);
            pStatement.setInt(2, idAlbum);
            pStatement.setInt(3, year);
            pStatement.setString(4, genre);
            pStatement.setString(5, audioFile);
            pStatement.setString(6, idUser.toString());
            int affectedRows = pStatement.executeUpdate();

            if (affectedRows == 0) {
                throw new SQLException("Creating song failed, no rows affected.");
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
                throw new SQLException("Creating song failed, no ID obtained.");
            }
        } finally {
            if (generatedKeys != null)
                try {
                    generatedKeys.close();
                } catch (SQLException e) {
                }
        }
        return newSong;
    }

    /**
     * Finds all songs uploaded by a specific user.
     *
     * @param userId The UUID of the user.
     * @return A list of songs uploaded by the user.
     * @throws SQLException if a database access error occurs.
     */
    public List<Song> findSongsByUser(UUID userId) throws SQLException {
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
            }
        }
        return songs;
    }

    /**
     * Finds all songs in the database.
     *
     * @return A list of all songs.
     * @throws SQLException if a database access error occurs.
     */
    public List<Song> findAllSongs() throws SQLException {
        List<Song> songs = new ArrayList<>();
        // Corrected query to fetch all songs without user filter
        String query = "SELECT idSong, title, idAlbum, year, genre, audioFile, BIN_TO_UUID(idUser) as idUser FROM Song";
        try (Statement statement = connection.createStatement(); // Use Statement for query without parameters
                ResultSet result = statement.executeQuery(query)) {
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
        }
        return songs;
    }

    /**
     * Initializes the SongRegistry with all songs from the database.
     * Should be called once at application startup.
     *
     * @param connection A valid database connection.
     * @throws SQLException          if a database access error occurs.
     * @throws IllegalStateException if the registry is already initialized.
     */
    public static synchronized void initializeRegistry(Connection connection) throws SQLException {
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
     * @throws SQLException if a database access error occurs.
     */
    public void deleteSong(int songId) throws SQLException, DAOException {
        // Remove from registry first (if initialized)
        if (SongRegistry.isInitialized()) {
            SongRegistry.removeSong(songId);
        } else {
            System.err.println("WARN: SongRegistry not initialized when deleting song ID: " + songId);
        }

        String query = "DELETE FROM Song WHERE idSong = ?";
        try (PreparedStatement pStatement = connection.prepareStatement(query)) {
            pStatement.setInt(1, songId);
            int affectedRows = pStatement.executeUpdate();
            if (affectedRows == 0) {
                throw new DAOException("Deleting song failed, no rows affected.", DAOException.DAOErrorType.NOT_FOUND);
            }
        }
    }
}
