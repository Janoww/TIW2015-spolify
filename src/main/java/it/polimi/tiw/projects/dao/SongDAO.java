
package it.polimi.tiw.projects.dao;

import it.polimi.tiw.projects.beans.Song;
import it.polimi.tiw.projects.exceptions.DAOException;
import it.polimi.tiw.projects.utils.Genre;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Data Access Object for managing songs in the database. Provides methods for creating, retrieving, updating, and deleting songs.
 */
public class SongDAO {
    private static final Logger logger = LoggerFactory.getLogger(SongDAO.class);
    private final Connection connection;

    public SongDAO(@NotNull Connection connection) {
        this.connection = connection;
    }

    /**
     * Creates a new song in the database.
     *
     * @param title     The title of the song.
     * @param idAlbum   The ID of the album the song belongs to.
     * @param genre     The genre of the song. (Can be null based on DB)
     * @param audioFile The path or URL to the audio file.
     * @param idUser    The UUID of the user who uploaded the song.
     * @return The newly created Song object with its generated ID.
     * @throws DAOException if the specified album ID does not exist
     *                      ({@link it.polimi.tiw.projects.exceptions.DAOException.DAOErrorType#NOT_FOUND})
     *                      or another database access error occurs
     *                      ({@link it.polimi.tiw.projects.exceptions.DAOException.DAOErrorType#GENERIC_ERROR}).
     */
    public Song createSong(@NotBlank String title, int idAlbum, Genre genre, @NotBlank String audioFile,
                           @NotNull UUID idUser) throws DAOException {
        logger.debug("Attempting to create song: title={}, idAlbum={}, genre={}, audioFile={}, userId={}", title,
                idAlbum, genre, audioFile, idUser);
        String query = "INSERT into Song (title, idAlbum, genre, audioFile, idUser) VALUES(?, ?, ?, ?, UUID_TO_BIN(?))";
        Song newSong;

        try (PreparedStatement pStatement = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            pStatement.setString(1, title);
            pStatement.setInt(2, idAlbum);
            if (genre != null) {
                pStatement.setString(3, genre.name());
            } else {
                pStatement.setNull(3, Types.VARCHAR);
            }
            pStatement.setString(4, audioFile);
            pStatement.setString(5, idUser.toString());
            int affectedRows = pStatement.executeUpdate();

            if (affectedRows == 0) {
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
                    newSong.setGenre(genre);
                    newSong.setAudioFile(audioFile);
                    newSong.setIdUser(idUser);
                    logger.info("Song created successfully with ID: {}", newId);

                } else {
                    logger.error("Creating song failed, no ID obtained for title={}, userId={}", title, idUser);
                    throw new DAOException("Creating song failed, no ID obtained.",
                            DAOException.DAOErrorType.GENERIC_ERROR);
                }
            }
        } catch (SQLException e) {
            // Check for foreign key constraint violation on idAlbum (MySQL error code 1452)
            if (e.getErrorCode() == 1452) { // NOT_FOUND
                logger.warn(
                        "Failed to create song title='{}', userId={}: Album ID {} not found. Details: SQLState={}, Message={}",
                        title, idUser, idAlbum, e.getSQLState(), e.getMessage());
                throw new DAOException("Album with ID " + idAlbum + " not found.", e,
                        DAOException.DAOErrorType.NOT_FOUND);
            } else { // GENERIC_ERROR
                logger.error("SQL error creating song title={}, userId={}: SQLState={}, ErrorCode={}, Message={}",
                        title, idUser, e.getSQLState(), e.getErrorCode(), e.getMessage(), e);
                throw new DAOException("Error creating song: " + e.getMessage(), e,
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
     * @throws DAOException if a database access error occurs
     *                      ({@link it.polimi.tiw.projects.exceptions.DAOException.DAOErrorType#GENERIC_ERROR}).
     */
    public List<Song> findSongsByUser(@NotNull UUID userId) throws DAOException {
        logger.debug("Attempting to find songs for user ID: {}", userId);
        List<Song> songs = new ArrayList<>();
        String query = "SELECT idSong, title, idAlbum, genre, audioFile, BIN_TO_UUID(idUser) as idUser FROM Song WHERE idUser = UUID_TO_BIN(?)";
        try (PreparedStatement pStatement = connection.prepareStatement(query)) {
            pStatement.setString(1, userId.toString());
            try (ResultSet result = pStatement.executeQuery()) {
                while (result.next()) {
                    Song song = new Song();
                    song.setIdSong(result.getInt("idSong"));
                    song.setTitle(result.getString("title"));
                    song.setIdAlbum(result.getInt("idAlbum"));
                    String genreStr = result.getString("genre");
                    if (genreStr != null) {
                        song.setGenre(Enum.valueOf(Genre.class, genreStr));
                    } else {
                        song.setGenre(null);
                    }
                    song.setAudioFile(result.getString("audioFile"));
                    song.setIdUser(UUID.fromString(result.getString("idUser")));
                    songs.add(song);
                }
                logger.debug("Found {} songs for user ID: {}", songs.size(), userId);
            }
        } catch (SQLException e) { // GENERIC_ERROR (unexpected)
            logger.error("SQL error finding songs for user ID {}: {}", userId, e.getMessage(), e);
            throw new DAOException("Error finding songs by user: " + e.getMessage(), e,
                    DAOException.DAOErrorType.GENERIC_ERROR);
        } catch (IllegalArgumentException e) {
            logger.error("Error parsing data (e.g., Genre, UUID) for songs for user ID {}: {}", userId, e.getMessage(),
                    e);
            throw new DAOException("Error parsing song data: " + e.getMessage(), e,
                    DAOException.DAOErrorType.GENERIC_ERROR);
        }
        return songs;
    }

    /**
     * Finds all songs in the database.
     *
     * @return A list of all songs.
     * @throws DAOException if a database access error occurs
     *                      ({@link it.polimi.tiw.projects.exceptions.DAOException.DAOErrorType#GENERIC_ERROR}).
     */
    public List<Song> findAllSongs() throws DAOException {
        logger.debug("Attempting to find all songs");
        List<Song> songs = new ArrayList<>();
        String query = "SELECT idSong, title, idAlbum, genre, audioFile, BIN_TO_UUID(idUser) as idUser FROM Song";
        try (Statement statement = connection.createStatement(); ResultSet result = statement.executeQuery(query)) {
            while (result.next()) {
                Song song = new Song();
                song.setIdSong(result.getInt("idSong"));
                song.setTitle(result.getString("title"));
                song.setIdAlbum(result.getInt("idAlbum"));
                String genreStr = result.getString("genre");
                if (genreStr != null) {
                    song.setGenre(Enum.valueOf(Genre.class, genreStr));
                } else {
                    song.setGenre(null);
                }
                song.setAudioFile(result.getString("audioFile"));
                song.setIdUser(UUID.fromString(result.getString("idUser")));
                songs.add(song);
            }
            logger.debug("Found {} songs in total.", songs.size());
        } catch (SQLException e) {
            logger.error("SQL error finding all songs: {}", e.getMessage(), e);
            throw new DAOException("Error finding all songs: " + e.getMessage(), e,
                    DAOException.DAOErrorType.GENERIC_ERROR);
        } catch (IllegalArgumentException e) {
            logger.error("Error parsing data (e.g., Genre, UUID) when finding all songs: {}", e.getMessage(), e);
            throw new DAOException("Error parsing song data: " + e.getMessage(), e,
                    DAOException.DAOErrorType.GENERIC_ERROR);
        }
        return songs;
    }

    /**
     * Deletes a song from the database.
     *
     * @param songId The ID of the song to delete.
     * @throws DAOException if the song is not found
     *                      ({@link it.polimi.tiw.projects.exceptions.DAOException.DAOErrorType#NOT_FOUND})
     *                      or another database access error occurs
     *                      ({@link it.polimi.tiw.projects.exceptions.DAOException.DAOErrorType#GENERIC_ERROR}).
     */
    public void deleteSong(int songId) throws DAOException {
        logger.debug("Attempting to delete song ID: {}", songId);

        String query = "DELETE FROM Song WHERE idSong = ?";
        try (PreparedStatement pStatement = connection.prepareStatement(query)) {
            pStatement.setInt(1, songId);
            int affectedRows = pStatement.executeUpdate();
            if (affectedRows == 0) {
                logger.warn("Deleting song ID {} from database failed (0 rows affected). Song might not exist in DB.",
                        songId);
                throw new DAOException("Deleting song failed, song ID " + songId + " not found in database.",
                        DAOException.DAOErrorType.NOT_FOUND);
            }
            logger.info("Song ID {} deleted successfully from database.", songId);
            // No return value needed, success is indicated by lack of exception
        } catch (SQLException e) {
            logger.error("SQL error deleting song ID {}: {}", songId, e.getMessage(), e);
            throw new DAOException("Error deleting song: " + e.getMessage(), e,
                    DAOException.DAOErrorType.GENERIC_ERROR);
        }
    }

    /**
     * Finds songs by a list of their IDs, ensuring they belong to a specific user.
     * Returns only the songs that match both the ID list and the user ID.
     *
     * @param songIds The list of song IDs to retrieve.
     * @param userId  The UUID of the user who must own the songs.
     * @return A list of {@link Song} objects matching the criteria. Returns an
     * empty list if songIds is null or empty, or if no matching songs are
     * found for this user.
     * @throws DAOException if a database access error occurs
     *                      ({@link it.polimi.tiw.projects.exceptions.DAOException.DAOErrorType#GENERIC_ERROR}).
     */
    public List<Song> findSongsByIdsAndUser(@NotNull List<Integer> songIds, @NotNull UUID userId) throws DAOException {
        logger.debug("Attempting to find songs by IDs: {} for user ID: {}", songIds, userId);

        if (songIds == null || songIds.isEmpty()) {
            logger.debug("Input song ID list is null or empty. Returning empty list.");
            return new ArrayList<>(); // Return empty list if no IDs provided
        }

        List<Song> songs = new ArrayList<>();
        // Dynamically build the IN clause for the prepared statement
        // Using StringBuilder for efficiency, especially with potentially long lists of
        // IDs.
        StringBuilder queryBuilder = new StringBuilder(
                "SELECT idSong, title, idAlbum, genre, audioFile, BIN_TO_UUID(idUser) as idUser FROM Song WHERE idUser = UUID_TO_BIN(?) AND idSong IN (");
        for (int i = 0; i < songIds.size(); i++) {
            queryBuilder.append("?");
            if (i < songIds.size() - 1) {
                queryBuilder.append(", "); // Add comma separator between placeholders
            }
        }
        queryBuilder.append(")"); // Close the IN clause parenthesis

        String query = queryBuilder.toString();
        logger.trace("Executing query: {}", query);

        try (PreparedStatement pStatement = connection.prepareStatement(query)) {
            // Set the user ID parameter (index 1)
            pStatement.setString(1, userId.toString());

            // Set the song ID parameters (starting from index 2)
            for (int i = 0; i < songIds.size(); i++) {
                // Parameter index is i + 2 because the first parameter (index 1) is the userId
                pStatement.setInt(i + 2, songIds.get(i));
            }

            try (ResultSet result = pStatement.executeQuery()) {
                while (result.next()) {
                    Song song = new Song();
                    song.setIdSong(result.getInt("idSong"));
                    song.setTitle(result.getString("title"));
                    song.setIdAlbum(result.getInt("idAlbum"));
                    String genreStr = result.getString("genre");
                    if (genreStr != null) {
                        song.setGenre(Enum.valueOf(Genre.class, genreStr));
                    } else {
                        song.setGenre(null);
                    }
                    song.setAudioFile(result.getString("audioFile"));
                    song.setIdUser(UUID.fromString(result.getString("idUser")));
                    songs.add(song);
                }
                logger.debug("Found {} songs matching IDs {} for user ID: {}", songs.size(), songIds, userId);
            }
        } catch (SQLException e) {
            throw new DAOException("Error finding songs by IDs and user: " + e.getMessage(), e,
                    DAOException.DAOErrorType.GENERIC_ERROR);
        }
        return songs;
    }
}
