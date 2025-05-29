package it.polimi.tiw.projects.tools;

import com.github.javafaker.Faker;
import it.polimi.tiw.projects.beans.Album;
import it.polimi.tiw.projects.beans.Song;
import it.polimi.tiw.projects.beans.User;
import it.polimi.tiw.projects.dao.*;
import it.polimi.tiw.projects.exceptions.DAOException;
import it.polimi.tiw.projects.utils.Genre;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.*;

public class MockDataGenerator {

    private static final Logger logger = LoggerFactory.getLogger(MockDataGenerator.class);

    // --- Configuration Constants ---
    // Database details from web.xml
    private static final String DB_URL = "jdbc:mysql://localhost:3306/TIW2025";
    private static final String DB_USER = "tiw";
    private static final String DB_PASSWORD = "TIW2025";
    private static final String DB_DRIVER = "com.mysql.cj.jdbc.Driver";

    private static final String MOCK_USER_USERNAME_PREFIX = "mockuser_";
    private static final String MOCK_USER_DEFAULT_PASSWORD = "password123";
    private static final int NUM_MOCK_USERS = 2;
    private static final int ALBUMS_PER_USER = 10;
    private static final int SONGS_PER_ALBUM = 5; // Total ~50 songs per user
    private static final int PLAYLISTS_PER_USER = 10;
    private static final int MIN_SONGS_PER_PLAYLIST = 5;
    private static final int MAX_SONGS_PER_PLAYLIST = 15;

    // Base storage path from AppContextListener logic
    private static final String STORAGE_BASE_DIRECTORY_PATH_CONFIG = System.getProperty("user.home") + "/Spolify";

    // Path to a real .ogg file in your project to use as the source for mock audio
    private static final String DUMMY_AUDIO_SOURCE_FILENAME = "valid.ogg";

    private static final Random random = new Random();
    private static final Faker faker = new Faker(new Locale.Builder().setLanguage("it").setRegion("IT").build(),
            random);

    private Connection connection;
    private UserDAO userDAO;
    private AlbumDAO albumDAO;
    private SongDAO songDAO;
    private PlaylistDAO playlistDAO;
    private AudioDAO audioDAO;
    private PlaylistOrderDAO playlistOrderDAO;

    public MockDataGenerator() {
        // Only used for creating mock data.
    }

    public static void main(String[] args) {
        if (args.length != 1 || (!"generate".equalsIgnoreCase(args[0]) && !"cleanup".equalsIgnoreCase(args[0]))) {
            logger.error(
                    "Usage: java -cp <classpath> it.polimi.tiw.projects.tools.MockDataGenerator <generate|cleanup>");
            System.exit(1);
        }

        logger.info("Attempting to run MockDataGenerator with action: {}", args[0]);
        logger.info("Using DB_URL: {}", DB_URL);
        logger.info("Using STORAGE_BASE_DIRECTORY_PATH_CONFIG: {}", STORAGE_BASE_DIRECTORY_PATH_CONFIG);
        logger.info("Using DUMMY_AUDIO_SOURCE_FILE_PATH_CONFIG: {}", DUMMY_AUDIO_SOURCE_FILENAME);

        MockDataGenerator generator = new MockDataGenerator();
        generator.performAction(args[0]);
    }

    private void setupDAOs() {
        // AudioDAO is initialized with the base storage path.
        this.audioDAO = new AudioDAO(Paths.get(STORAGE_BASE_DIRECTORY_PATH_CONFIG));
        this.playlistOrderDAO = new PlaylistOrderDAO(Paths.get(STORAGE_BASE_DIRECTORY_PATH_CONFIG));
        this.userDAO = new UserDAO(connection);
        this.albumDAO = new AlbumDAO(connection);
        this.songDAO = new SongDAO(connection);
        this.playlistDAO = new PlaylistDAO(connection);
    }

    public void performAction(String action) {
        try {
            Class.forName(DB_DRIVER); // Ensure JDBC driver is loaded
            connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
            connection.setAutoCommit(false);
            setupDAOs();

            if ("generate".equalsIgnoreCase(action)) {
                generateData();
            } else if ("cleanup".equalsIgnoreCase(action)) {
                cleanupData();
            } else {
                logger.warn("Invalid action. Use 'generate' or 'cleanup'.");
            }

            connection.commit();
            logger.info("Action '{}' completed successfully.", action);

        } catch (Exception e) {
            logger.error("Error during mock data {}: {}", action, e.getMessage(), e);
            if (connection != null) {
                try {
                    connection.rollback();
                    logger.info("Transaction rolled back.");
                } catch (SQLException ex) {
                    logger.error("Error rolling back transaction: {}", ex.getMessage(), ex);
                }
            }
        } finally {
            if (connection != null) {
                try {
                    connection.setAutoCommit(true); // Reset autoCommit
                    connection.close();
                } catch (SQLException e) {
                    logger.error("Error closing connection: {}", e.getMessage(), e);
                }
            }
            // Attempt to shutdown the MySQL abandoned connection cleanup thread
            // This is often the cause of the lingering thread warning when the app exits.
            try {
                com.mysql.cj.jdbc.AbandonedConnectionCleanupThread.checkedShutdown();
                logger.info("MySQL AbandonedConnectionCleanupThread shutdown attempted.");
            } catch (Exception e) {
                logger.warn("Exception during MySQL AbandonedConnectionCleanupThread shutdown: {}", e.getMessage());
            }
        }
    }

    private void generateData() throws DAOException, IOException {
        logger.info("Starting mock data generation...");

        // Save the dummy audio file ONCE to be shared by all mock songs
        String singleSharedAudioFilename;
        try (InputStream audioStream = this.getClass().getClassLoader()
                .getResourceAsStream(DUMMY_AUDIO_SOURCE_FILENAME)) {
            if (audioStream == null) {
                throw new IOException(
                        "Cannot find dummy audio file '" + DUMMY_AUDIO_SOURCE_FILENAME + "' in classpath resources.");
            }
            singleSharedAudioFilename = audioDAO.saveAudio(audioStream, DUMMY_AUDIO_SOURCE_FILENAME);
            logger.info("Shared dummy audio file saved as: {}", singleSharedAudioFilename);
        } catch (IOException e) {
            logger.error("Failed to save the shared dummy audio file: {}", e.getMessage(), e);
            throw e; // Re-throw to stop generation if shared audio can't be prepared
        }

        for (int i = 0; i < NUM_MOCK_USERS; i++) {
            String username = MOCK_USER_USERNAME_PREFIX + i;
            String name = "Mock";
            String surname = "User" + i;

            User existingUser = userDAO.findUserByUsername(username);
            if (existingUser != null) {
                logger.info("Mock user {} already exists. Skipping generation for this user.", username);
                continue;
            }

            User mockUser = userDAO.createUser(username, MOCK_USER_DEFAULT_PASSWORD, name, surname);
            UUID userId = mockUser.getIdUser();
            logger.info("Created user: {} (ID: {})", username, userId);

            List<Integer> userSongIds = new ArrayList<>();

            for (int j = 0; j < ALBUMS_PER_USER; j++) {
                String albumName = faker.book().title();
                if (j % 3 == 0) {
                    albumName = faker.music().genre() + " Anthems Vol. " + (j / 3 + 1);
                }
                String albumArtist = faker.rockBand().name();
                int albumYear = 2020 + random.nextInt(5); // Years 2020-2024

                Album createdAlbum = albumDAO.createAlbum(albumName, albumYear, albumArtist, null, userId);
                int albumId = createdAlbum.getIdAlbum();
                logger.info("  Created album: {} (ID: {}) by {} for user {}", albumName, albumId, albumArtist,
                        username);

                for (int k = 0; k < SONGS_PER_ALBUM; k++) {
                    String songTitle = faker.funnyName().name();
                    if (songTitle.length() > 100)
                        songTitle = songTitle.substring(0, 100);

                    Genre genre = Genre.values()[random.nextInt(Genre.values().length)];

                    Song createdSong = songDAO.createSong(songTitle, albumId, genre, singleSharedAudioFilename, userId);
                    userSongIds.add(createdSong.getIdSong());
                    logger.info("    Created song: {} (ID: {}, File: {}) for album {}", songTitle,
                            createdSong.getIdSong(), singleSharedAudioFilename, albumName);
                }
            }

            // Create Playlists
            if (!userSongIds.isEmpty()) {
                for (int l = 0; l < PLAYLISTS_PER_USER; l++) {
                    String playlistName = faker.ancient().primordial() + " Mix Vol. " + (l + 1);
                    if (playlistName.length() > 100)
                        playlistName = playlistName.substring(0, 100);

                    Collections.shuffle(userSongIds); // Shuffle to get random songs for each playlist

                    int numSongsInPlaylist = MIN_SONGS_PER_PLAYLIST
                            + random.nextInt(MAX_SONGS_PER_PLAYLIST - MIN_SONGS_PER_PLAYLIST + 1);
                    numSongsInPlaylist = Math.min(numSongsInPlaylist, userSongIds.size());

                    List<Integer> playlistSongIds = new ArrayList<>(userSongIds.subList(0, numSongsInPlaylist));

                    // Assuming PlaylistDAO.createPlaylist can take a list of song IDs
                    // If it returns the created Playlist object, you can log its ID.
                    playlistDAO.createPlaylist(playlistName, userId, playlistSongIds);
                    logger.info("  Created playlist: {} for user {} with {} songs", playlistName, username,
                            playlistSongIds.size());
                }
            } else {
                logger.warn("  No songs available for user {} to create playlists.", username);
            }
        }
    }

    private void cleanupData() throws DAOException {
        logger.info("Starting mock data cleanup...");
        for (int i = 0; i < NUM_MOCK_USERS; i++) {
            String username = MOCK_USER_USERNAME_PREFIX + i;
            User mockUser = userDAO.findUserByUsername(username);
            if (mockUser != null) {
                UUID userId = mockUser.getIdUser();
                logger.info("Cleaning up data for user: {} (ID: {})", username, userId);

                // 1. Get all playlists for the user to delete their order files
                List<it.polimi.tiw.projects.beans.Playlist> userPlaylists = playlistDAO.findPlaylistsByUser(userId);
                if (userPlaylists != null && !userPlaylists.isEmpty()) {
                    logger.debug("Found {} playlists for user {}. Deleting their order files...", userPlaylists.size(),
                            username);
                    for (it.polimi.tiw.projects.beans.Playlist p : userPlaylists) {
                        try {
                            playlistOrderDAO.deletePlaylistOrder(p.getIdPlaylist());
                            logger.info("Deleted order file for playlist ID: {}", p.getIdPlaylist());
                        } catch (DAOException e) {
                            // Log error but continue cleanup, as file might not exist or other non-critical
                            // issue
                            logger.warn("Could not delete order file for playlist ID {}: {}. Continuing cleanup.",
                                    p.getIdPlaylist(), e.getMessage());
                        }
                    }
                } else {
                    logger.debug("No playlists found for user {} to clean up order files.", username);
                }

                // 2. Delete the user (DB cascade should handle albums, songs,
                // playlist_metadata, playlist_content)
                userDAO.deleteUser(userId);
                logger.info("Deleted user: {} and their associated DB data (via DB cascade).", username);
            } else {
                logger.info("Mock user {} not found for cleanup.", username);
            }
        }
    }
}
