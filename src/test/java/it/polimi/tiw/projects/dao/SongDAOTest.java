package it.polimi.tiw.projects.dao;

import it.polimi.tiw.projects.beans.Album;
import it.polimi.tiw.projects.beans.Song;
import it.polimi.tiw.projects.beans.User;
import it.polimi.tiw.projects.exceptions.DAOException;
import it.polimi.tiw.projects.utils.Genre;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class SongDAOTest {

    private static final Logger logger = LoggerFactory.getLogger(SongDAOTest.class);
    // Database connection details
    private static final String DB_URL = "jdbc:mysql://localhost:3306/TIW2025";
    private static final String DB_USER = "tiw";
    private static final String DB_PASS = "TIW2025";
    // Test Data - Use unique values
    private static final String TEST_SONG_TITLE_1 = "JUnit Test Song 1 - " + System.currentTimeMillis();
    private static final String TEST_SONG_TITLE_2 = "JUnit Test Song 2 - " + System.currentTimeMillis();
    private static final Genre TEST_GENRE = Genre.POP;
    private static final String TEST_AUDIO_FILE_1 = "/audio/junit_test1.mp3";
    private static final String TEST_AUDIO_FILE_2 = "/audio/junit_test2.mp3";
    private static final String TEST_AUDIO_FILE_3 = "/audio/junit_test3.mp3";
    // Test User details (needed for song creation) - User 1
    private static final String TEST_USERNAME = "song_test_user_junit_1";
    private static final String TEST_PASSWORD = "password_song_1";
    private static final String TEST_NAME = "SongJUnit1";
    private static final String TEST_SURNAME = "Tester1";
    // Second Test User details - User 2
    private static final String TEST_USERNAME_2 = "song_test_user_junit_2";
    private static final String TEST_PASSWORD_2 = "password_song_2";
    private static final String TEST_NAME_2 = "SongJUnit2";
    private static final String TEST_SURNAME_2 = "Tester2";
    // Test Album details
    private static final String TEST_ALBUM_TITLE = "JUnit Test Album - " + System.currentTimeMillis();
    private static final String TEST_ALBUM_ARTIST = "JUnit Artist";
    private static final int TEST_ALBUM_YEAR = 2024;
    private static Connection connection;
    private static SongDAO songDAO;
    private static UUID testUserId;
    private static UUID testUserId2; // For authorization tests
    private static Integer testAlbumId = null;

    private Integer createdSongId1 = null;
    private Integer createdSongId2 = null;
    private Integer createdSongId3 = null;

    @BeforeAll
    void setUpClass() throws SQLException, DAOException {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
            connection.setAutoCommit(false); // Manage transactions manually
            songDAO = new SongDAO(connection);
            // To manage a test user if needed
            UserDAO userDAO = new UserDAO(connection); // Initialize UserDAO for test user setup
            // To manage a test album
            AlbumDAO albumDAO = new AlbumDAO(connection); // Initialize AlbumDAO
            logger.info("Database connection established for SongDAOTest.");

            // Initial cleanup of potential leftover test data (order matters)
            cleanupTestSongs();
            cleanupTestAlbum(); // Clean up test album first due to FK
            cleanupTestUsers(); // Clean up both test users
            connection.commit();

            // Create the first test user required for creating songs/albums
            userDAO.createUser(TEST_USERNAME, TEST_PASSWORD, TEST_NAME, TEST_SURNAME);
            connection.commit(); // Commit user creation
            User testUser1 = userDAO.checkCredentials(TEST_USERNAME, TEST_PASSWORD);
            assertNotNull(testUser1, "Test user 1 could not be created or found.");
            testUserId = testUser1.getIdUser();
            logger.info("Test user 1 created with ID: {}", testUserId);

            // Create the second test user for authorization tests
            userDAO.createUser(TEST_USERNAME_2, TEST_PASSWORD_2, TEST_NAME_2, TEST_SURNAME_2);
            connection.commit(); // Commit user creation
            User testUser2 = userDAO.checkCredentials(TEST_USERNAME_2, TEST_PASSWORD_2);
            assertNotNull(testUser2, "Test user 2 could not be created or found.");
            testUserId2 = testUser2.getIdUser();
            logger.info("Test user 2 created with ID: {}", testUserId2);

            // Create the test album required for creating songs (pass null for image)
            Album testAlbum = albumDAO.createAlbum(TEST_ALBUM_TITLE, TEST_ALBUM_YEAR, TEST_ALBUM_ARTIST, null,
                    testUserId);
            connection.commit(); // Commit album creation
            assertNotNull(testAlbum, "Test album could not be created.");
            testAlbumId = testAlbum.getIdAlbum();
            assertNotNull(testAlbumId, "Test album ID is null after creation.");
            logger.info("Test album created with ID: {}", testAlbumId);

        } catch (ClassNotFoundException e) {
            logger.error("MySQL JDBC Driver not found", e);
            throw new SQLException("MySQL JDBC Driver not found.", e);
        } catch (SQLException e) {
            logger.error("Failed to connect to the database '{}'", DB_URL, e);
            throw e;
        } catch (DAOException e) {
            logger.error("DAOException during test user setup", e);
            if (connection != null)
                connection.rollback(); // Rollback if user creation failed
            throw e;
        }
    }

    @AfterAll
    void tearDownClass() throws SQLException {
        if (connection != null && !connection.isClosed()) {
            try {
                // Final cleanup (order matters due to FK constraints)
                cleanupTestSongs(); // Songs depend on Album and Users
                cleanupTestAlbum(); // Album depends on User
                cleanupTestUsers(); // Clean up both users
                connection.commit(); // Commit final cleanup
            } catch (SQLException | NullPointerException e) { // Catch potential NPE if setup failed
                // badly
                logger.error("Error during final cleanup", e);
                connection.rollback(); // Attempt rollback on cleanup error
            } finally {
                connection.close();
                logger.info("Database connection closed for SongDAOTest.");
            }
        }
    }

    @BeforeEach
    void setUp() throws SQLException {
        // Clean songs before each test, commit the cleanup
        cleanupTestSongs();
        connection.commit();
        createdSongId1 = null; // Reset stored IDs for user 1
        createdSongId2 = null;
        createdSongId3 = null; // Reset stored ID for user 2
    }

    @AfterEach
    void tearDown() throws SQLException {
        // Rollback any uncommitted changes from the test itself
        connection.rollback();
        // Clean up songs created during the test, commit the cleanup
        cleanupTestSongs();
        connection.commit();
    }

    // Helper method to delete test songs based on stored IDs or title pattern
    private void cleanupTestSongs() throws SQLException {
        // Delete by specific ID if known
        if (createdSongId1 != null) {
            deleteSongById(createdSongId1);
            createdSongId1 = null;
        }
        if (createdSongId2 != null) {
            deleteSongById(createdSongId2);
            createdSongId2 = null;
        }
        if (createdSongId3 != null) {
            deleteSongById(createdSongId3);
            createdSongId3 = null;
        }
        // Robust cleanup: Delete any songs matching the test title patterns or
        // belonging to test users
        String deleteSQL = "DELETE FROM Song WHERE title LIKE ? OR idUser = UUID_TO_BIN(?) OR idUser = UUID_TO_BIN(?)";
        try (PreparedStatement pStatement = connection.prepareStatement(deleteSQL)) {
            pStatement.setString(1, "JUnit Test Song %"); // Pattern matching
            pStatement.setString(2, testUserId != null ? testUserId.toString() : UUID.randomUUID().toString()); // Avoid
            // null
            // UUID
            pStatement.setString(3, testUserId2 != null ? testUserId2.toString() : UUID.randomUUID().toString()); // Avoid
            // null
            // UUID
            pStatement.executeUpdate();
        } catch (NullPointerException e) {
            logger.warn("NPE during song cleanup, likely testUserId not set", e); // Use WARN for
            // potential
            // issues
            // Continue cleanup if possible
        }
    }

    private void deleteSongById(int songId) throws SQLException {
        String deleteSQL = "DELETE FROM Song WHERE idSong = ?";
        try (PreparedStatement pStatement = connection.prepareStatement(deleteSQL)) {
            pStatement.setInt(1, songId);
            pStatement.executeUpdate();
        }
    }

    // Helper method to delete the test users
    private void cleanupTestUsers() throws SQLException {
        String deleteSQL = "DELETE FROM User WHERE username = ? OR username = ?";
        try (PreparedStatement pStatement = connection.prepareStatement(deleteSQL)) {
            pStatement.setString(1, TEST_USERNAME);
            pStatement.setString(2, TEST_USERNAME_2);
            pStatement.executeUpdate();
        }
    }

    // Helper method to delete the test album
    private void cleanupTestAlbum() throws SQLException {
        // First, ensure songs referencing the album are deleted (should be handled by
        // cleanupTestSongs)
        // Then delete the album itself
        if (testAlbumId != null) {
            deleteAlbumById(testAlbumId);
        }
        // Robust cleanup: Delete any albums matching the test name pattern or user ID
        String deleteSQL = "DELETE FROM Album WHERE name = ? OR idUser = UUID_TO_BIN(?)";
        try (PreparedStatement pStatement = connection.prepareStatement(deleteSQL)) {
            pStatement.setString(1, TEST_ALBUM_TITLE); // TEST_ALBUM_TITLE holds the name used for
            // creation
            pStatement.setString(2, testUserId != null ? testUserId.toString() : UUID.randomUUID().toString()); // Clean
            // albums
            // by
            // user
            // 1
            pStatement.executeUpdate();
        } catch (NullPointerException e) {
            logger.warn("NPE during album cleanup, likely testUserId not set", e); // Use WARN for
            // potential
            // issues
            // Continue cleanup if possible
        }
        testAlbumId = null; // Reset after deletion attempts
    }

    private void deleteAlbumById(int albumId) throws SQLException {
        String deleteSQL = "DELETE FROM Album WHERE idAlbum = ?";
        try (PreparedStatement pStatement = connection.prepareStatement(deleteSQL)) {
            pStatement.setInt(1, albumId);
            pStatement.executeUpdate();
        }
    }

    // --- Test Cases ---

    @Test
    @Order(1)
    @DisplayName("Test successful song creation")
    void testCreateSong_Success() throws SQLException {
        assertNotNull(testAlbumId, "Test Album ID must be set before creating a song.");
        Song createdSong = assertDoesNotThrow(
                () -> songDAO.createSong(TEST_SONG_TITLE_1, testAlbumId, TEST_GENRE, TEST_AUDIO_FILE_1, testUserId));

        assertNotNull(createdSong, "Created song object should not be null.");
        assertTrue(createdSong.getIdSong() > 0, "Created song ID should be positive.");
        createdSongId1 = createdSong.getIdSong(); // Store ID for cleanup/verification

        assertEquals(TEST_SONG_TITLE_1, createdSong.getTitle());
        assertEquals(testAlbumId, createdSong.getIdAlbum()); // Use dynamic album ID
        assertEquals(TEST_GENRE, createdSong.getGenre());
        assertEquals(TEST_AUDIO_FILE_1, createdSong.getAudioFile());
        assertEquals(testUserId, createdSong.getIdUser());

        // Verify data in DB before commit (within the same transaction)
        Song foundSong = findSongByIdDirectly(createdSongId1);
        assertNotNull(foundSong, "Song should be findable in DB immediately after creation.");
        assertEquals(TEST_SONG_TITLE_1, foundSong.getTitle());

        connection.commit(); // Commit the transaction

        // Verify data in DB after commit
        Song foundSongAfterCommit = findSongByIdDirectly(createdSongId1);
        assertNotNull(foundSongAfterCommit, "Song should be findable in DB after commit.");
        assertEquals(TEST_SONG_TITLE_1, foundSongAfterCommit.getTitle());
    }

    // Removed testInitializeRegistry_Success as the registry is gone

    @Test
    @Order(2)
    @DisplayName("Test finding songs by user")
    void testFindSongsByUser_Success() throws DAOException, SQLException {
        assertNotNull(testAlbumId, "Test Album ID must be set before creating songs.");
        // Create two songs for the test user
        Song song1 = songDAO.createSong(TEST_SONG_TITLE_1, testAlbumId, TEST_GENRE, TEST_AUDIO_FILE_1, testUserId);
        Song song2 = songDAO.createSong(TEST_SONG_TITLE_2, testAlbumId, TEST_GENRE, // Assuming same genre for
                                                                                    // simplicity, year is from album
                TEST_AUDIO_FILE_2, testUserId);
        createdSongId1 = song1.getIdSong();
        createdSongId2 = song2.getIdSong();
        connection.commit();

        List<Song> userSongs = assertDoesNotThrow(() -> songDAO.findSongsByUser(testUserId));

        assertNotNull(userSongs);
        assertEquals(2, userSongs.size(), "Should find exactly 2 songs for the test user.");

        // Check if the found songs match the created ones (order might vary)
        assertTrue(userSongs.stream()
                .anyMatch(s -> s.getIdSong() == createdSongId1 && s.getTitle().equals(TEST_SONG_TITLE_1)));
        assertTrue(userSongs.stream()
                .anyMatch(s -> s.getIdSong() == createdSongId2 && s.getTitle().equals(TEST_SONG_TITLE_2)));
    }

    @Test
    @Order(3)
    @DisplayName("Test finding songs by user when none exist")
    void testFindSongsByUser_Empty() {
        // Ensure no songs exist for a different random user ID
        UUID nonExistentUserId = UUID.randomUUID();
        List<Song> userSongs = assertDoesNotThrow(() -> songDAO.findSongsByUser(nonExistentUserId));

        assertNotNull(userSongs);
        assertTrue(userSongs.isEmpty(), "Should find no songs for a user who hasn't added any.");
    }

    @Test
    @Order(4)
    @DisplayName("Test finding all songs")
    void testFindAllSongs_Success() throws DAOException, SQLException {
        assertNotNull(testAlbumId, "Test Album ID must be set before creating songs.");
        // Create two songs
        Song song1 = songDAO.createSong(TEST_SONG_TITLE_1, testAlbumId, TEST_GENRE, TEST_AUDIO_FILE_1, testUserId);
        Song song2 = songDAO.createSong(TEST_SONG_TITLE_2, testAlbumId, TEST_GENRE, // Assuming same genre
                TEST_AUDIO_FILE_2, testUserId);
        createdSongId1 = song1.getIdSong();
        createdSongId2 = song2.getIdSong();
        connection.commit();

        List<Song> allSongs = assertDoesNotThrow(() -> songDAO.findAllSongs());

        assertNotNull(allSongs);
        // Note: This assertion depends on the state of the DB. It should find AT LEAST
        // the two created songs.
        assertTrue(allSongs.size() >= 2, "Should find at least the 2 created test songs.");

        // Verify our test songs are present
        assertTrue(allSongs.stream()
                .anyMatch(s -> s.getIdSong() == createdSongId1 && s.getTitle().equals(TEST_SONG_TITLE_1)));
        assertTrue(allSongs.stream()
                .anyMatch(s -> s.getIdSong() == createdSongId2 && s.getTitle().equals(TEST_SONG_TITLE_2)));
    }

    @Test
    @Order(5)
    @DisplayName("Test finding all songs when DB is empty (after cleanup)")
    void testFindAllSongs_Empty() throws SQLException {
        // Cleanup ensures no test songs are present. Assuming DB might have other
        // songs.
        // To truly test empty, we'd need to wipe the table, which is risky.
        // This test relies on the @BeforeEach cleanup.
        cleanupTestSongs(); // Ensure our test songs are gone
        connection.commit();

        List<Song> allSongs = assertDoesNotThrow(() -> songDAO.findAllSongs());
        assertNotNull(allSongs);

        // Check that *our* test songs are not present
        assertFalse(allSongs.stream().anyMatch(s -> s.getTitle().startsWith("JUnit Test Song")),
                "No JUnit test songs should be found after cleanup.");
    }

    @Test
    @Order(6)
    @DisplayName("Test deleting a song successfully")
    void testDeleteSong_Success() throws DAOException, SQLException {
        assertNotNull(testAlbumId, "Test Album ID must be set before creating a song.");
        // Create a song
        Song songToDelete = songDAO.createSong(TEST_SONG_TITLE_1, testAlbumId, TEST_GENRE, TEST_AUDIO_FILE_1,
                testUserId);
        createdSongId1 = songToDelete.getIdSong(); // Store ID for verification
        connection.commit();

        // Verify it exists before delete
        assertNotNull(findSongByIdDirectly(createdSongId1), "Song should exist before deletion.");

        // Delete the song - Now returns void
        assertDoesNotThrow(() -> songDAO.deleteSong(createdSongId1),
                "Successful delete should not throw an exception.");
        connection.commit(); // Commit the deletion

        // Verify it's gone
        assertNull(findSongByIdDirectly(createdSongId1), "Song should not be findable after deletion.");
        createdSongId1 = null; // Nullify ID as it's successfully deleted (cleanup won't find it)
    }

    @Test
    @Order(7)
    @DisplayName("Test deleting a non-existent song")
    void testDeleteSong_NotFound() {
        int nonExistentSongId = -999; // An ID that certainly doesn't exist

        DAOException exception = assertThrows(DAOException.class, () -> {
            songDAO.deleteSong(nonExistentSongId);
            // Rollback will happen in @AfterEach if commit wasn't reached
        });

        // Updated expected message based on SongDAO implementation
        assertEquals("Deleting song failed, song ID " + nonExistentSongId + " not found in database.",
                exception.getMessage());
        assertEquals(DAOException.DAOErrorType.NOT_FOUND, exception.getErrorType());
    }

    @Test
    @Order(8)
    @DisplayName("Test creating a song with a non-existent album ID")
    void testCreateSong_AlbumNotFound() {
        assertNotNull(testUserId, "Test User ID must be set.");
        int nonExistentAlbumId = -999;

        DAOException exception = assertThrows(DAOException.class, () -> {
            songDAO.createSong(TEST_SONG_TITLE_1, nonExistentAlbumId, TEST_GENRE, TEST_AUDIO_FILE_1, testUserId);
            // Rollback will happen in @AfterEach
        }, "Creating a song with a non-existent album ID should throw DAOException.");

        assertEquals(DAOException.DAOErrorType.NOT_FOUND, exception.getErrorType(),
                "Exception type should be NOT_FOUND.");
        assertTrue(exception.getMessage().contains("Album with ID " + nonExistentAlbumId + " not found."),
                "Exception message should indicate album not found.");
    }

    @Test
    @Order(9)
    @DisplayName("Test findSongsByIdsAndUser - Success")
    void testFindSongsByIdsAndUser_Success() throws DAOException, SQLException {
        assertNotNull(testAlbumId, "Test Album ID must be set.");
        assertNotNull(testUserId, "Test User ID must be set.");

        // Create two songs for user 1
        Song song1 = songDAO.createSong(TEST_SONG_TITLE_1, testAlbumId, TEST_GENRE, TEST_AUDIO_FILE_1, testUserId);
        Song song2 = songDAO.createSong(TEST_SONG_TITLE_2, testAlbumId, TEST_GENRE, TEST_AUDIO_FILE_2, testUserId);
        createdSongId1 = song1.getIdSong();
        createdSongId2 = song2.getIdSong();
        connection.commit();

        List<Integer> requestedIds = List.of(createdSongId1, createdSongId2);
        List<Song> foundSongs = assertDoesNotThrow(() -> songDAO.findSongsByIdsAndUser(requestedIds, testUserId));

        assertNotNull(foundSongs);
        assertEquals(2, foundSongs.size(), "Should find exactly 2 songs.");
        assertTrue(foundSongs.stream().anyMatch(s -> s.getIdSong() == createdSongId1), "Song 1 should be present.");
        assertTrue(foundSongs.stream().anyMatch(s -> s.getIdSong() == createdSongId2), "Song 2 should be present.");
    }

    @Test
    @Order(10)
    @DisplayName("Test findSongsByIdsAndUser - Partial Match (Valid and Invalid IDs)")
    void testFindSongsByIdsAndUser_PartialMatch() throws DAOException, SQLException {
        assertNotNull(testAlbumId, "Test Album ID must be set.");
        assertNotNull(testUserId, "Test User ID must be set.");
        int nonExistentId = -555;

        // Create one song for user 1
        Song song1 = songDAO.createSong(TEST_SONG_TITLE_1, testAlbumId, TEST_GENRE, TEST_AUDIO_FILE_1, testUserId);
        createdSongId1 = song1.getIdSong();
        connection.commit();

        List<Integer> requestedIds = List.of(createdSongId1, nonExistentId);
        List<Song> foundSongs = assertDoesNotThrow(() -> songDAO.findSongsByIdsAndUser(requestedIds, testUserId));

        assertNotNull(foundSongs);
        assertEquals(1, foundSongs.size(), "Should find only the 1 valid song.");
        assertEquals(createdSongId1, foundSongs.getFirst().getIdSong(), "The found song should be song 1.");
    }

    @Test
    @Order(11)
    @DisplayName("Test findSongsByIdsAndUser - Authorization (Mix of Own and Other's Songs)")
    void testFindSongsByIdsAndUser_Authorization() throws DAOException, SQLException {
        assertNotNull(testAlbumId, "Test Album ID must be set.");
        assertNotNull(testUserId, "Test User ID 1 must be set.");
        assertNotNull(testUserId2, "Test User ID 2 must be set.");

        // Create one song for user 1
        Song song1 = songDAO.createSong(TEST_SONG_TITLE_1, testAlbumId, TEST_GENRE, TEST_AUDIO_FILE_1, testUserId);
        createdSongId1 = song1.getIdSong();
        // Create one song for user 2
        Song song3 = songDAO.createSong(TEST_SONG_TITLE_2, testAlbumId, TEST_GENRE, TEST_AUDIO_FILE_3, testUserId2);
        createdSongId3 = song3.getIdSong();
        connection.commit();

        // Request both songs, but as user 1
        List<Integer> requestedIds = List.of(createdSongId1, createdSongId3);
        List<Song> foundSongs = assertDoesNotThrow(() -> songDAO.findSongsByIdsAndUser(requestedIds, testUserId));

        assertNotNull(foundSongs);
        assertEquals(1, foundSongs.size(), "Should find only the 1 song belonging to user 1.");
        assertEquals(createdSongId1, foundSongs.getFirst().getIdSong(),
                "The found song should be song 1 (owned by user 1).");
        assertEquals(testUserId, foundSongs.getFirst().getIdUser(), "Found song must belong to user 1.");
    }

    @Test
    @Order(12)
    @DisplayName("Test findSongsByIdsAndUser - Empty or Null Input List")
    void testFindSongsByIdsAndUser_EmptyInput() {
        assertNotNull(testUserId, "Test User ID must be set.");

        // Test with empty list
        List<Integer> emptyList = List.of();
        List<Song> foundSongsEmpty = assertDoesNotThrow(() -> songDAO.findSongsByIdsAndUser(emptyList, testUserId));
        assertNotNull(foundSongsEmpty);
        assertTrue(foundSongsEmpty.isEmpty(), "Result should be empty for an empty input list.");

        // Test with null list
        List<Song> foundSongsNull = assertDoesNotThrow(() -> songDAO.findSongsByIdsAndUser(null, testUserId));
        assertNotNull(foundSongsNull);
        assertTrue(foundSongsNull.isEmpty(), "Result should be empty for a null input list.");
    }

    @Test
    @Order(13)
    @DisplayName("Test findSongsByIdsAndUser - No Matching Songs (Wrong User)")
    void testFindSongsByIdsAndUser_NoMatch() throws DAOException, SQLException {
        assertNotNull(testAlbumId, "Test Album ID must be set.");
        assertNotNull(testUserId, "Test User ID 1 must be set.");
        assertNotNull(testUserId2, "Test User ID 2 must be set.");

        // Create one song for user 2
        Song song3 = songDAO.createSong(TEST_SONG_TITLE_2, testAlbumId, TEST_GENRE, TEST_AUDIO_FILE_3, testUserId2);
        createdSongId3 = song3.getIdSong();
        connection.commit();

        // Request user 2's song, but as user 1
        List<Integer> requestedIds = List.of(createdSongId3);
        List<Song> foundSongs = assertDoesNotThrow(() -> songDAO.findSongsByIdsAndUser(requestedIds, testUserId));

        assertNotNull(foundSongs);
        assertTrue(foundSongs.isEmpty(), "Should find no songs when requesting another user's song ID.");
    }

    @Test
    @Order(14)
    @DisplayName("Test creating a song with a non-existent user ID")
    void testCreateSong_WithNonExistentUserId() {
        assertNotNull(testAlbumId, "Test Album ID must be set.");
        UUID nonExistentUserId = UUID.randomUUID();

        DAOException exception = assertThrows(
                DAOException.class, () -> songDAO.createSong(TEST_SONG_TITLE_1, testAlbumId, TEST_GENRE,
                        TEST_AUDIO_FILE_1, nonExistentUserId),
                "Creating a song with a non-existent user ID should throw DAOException.");

        assertTrue(
                exception.getErrorType() == DAOException.DAOErrorType.GENERIC_ERROR
                        || exception.getErrorType() == DAOException.DAOErrorType.CONSTRAINT_VIOLATION
                        || exception.getErrorType() == DAOException.DAOErrorType.NOT_FOUND,
                "Exception type should indicate a database error due to FK violation on userId.");
        logger.info("Caught expected exception for non-existent user ID: {}", exception.getMessage());
    }

    @Test
    @Order(15)
    @DisplayName("Test findSongsByUser with a non-existent user ID")
    void testFindSongsByUser_NonExistentUser() throws DAOException {
        UUID nonExistentUserId = UUID.randomUUID();
        List<Song> songs = songDAO.findSongsByUser(nonExistentUserId);
        assertNotNull(songs, "Song list should not be null even for non-existent user.");
        assertTrue(songs.isEmpty(), "Song list should be empty for a non-existent user.");
    }

    @Test
    @Order(16)
    @DisplayName("Test findSongsByIdsAndUser with a non-existent user ID")
    void testFindSongsByIdsAndUser_NonExistentUser() throws DAOException, SQLException {
        assertNotNull(testAlbumId, "Test Album ID must be set.");
        assertNotNull(testUserId, "Test User ID 1 must be set.");

        // Create a song for user 1
        Song song1 = songDAO.createSong(TEST_SONG_TITLE_1, testAlbumId, TEST_GENRE, TEST_AUDIO_FILE_1, testUserId);
        createdSongId1 = song1.getIdSong();
        connection.commit();

        UUID nonExistentUserId = UUID.randomUUID();
        List<Integer> requestedIds = List.of(createdSongId1);

        List<Song> foundSongs = songDAO.findSongsByIdsAndUser(requestedIds, nonExistentUserId);
        assertNotNull(foundSongs, "Song list should not be null.");
        assertTrue(foundSongs.isEmpty(), "Should find no songs for a non-existent user, even if song IDs are valid.");
    }

    // --- Helper method for direct DB verification ---
    private Song findSongByIdDirectly(int songId) throws SQLException {
        String query = "SELECT idSong, title, idAlbum, genre, audioFile, BIN_TO_UUID(idUser) as idUser FROM Song WHERE idSong = ?";
        try (PreparedStatement pStatement = connection.prepareStatement(query)) {
            pStatement.setInt(1, songId);
            try (ResultSet result = pStatement.executeQuery()) {
                if (result.next()) {
                    Song song = new Song();
                    song.setIdSong(result.getInt("idSong"));
                    song.setTitle(result.getString("title"));
                    song.setIdAlbum(result.getInt("idAlbum"));
                    // Convert the string from DB to Genre enum
                    song.setGenre(Enum.valueOf(Genre.class, result.getString("genre")));
                    song.setAudioFile(result.getString("audioFile"));
                    song.setIdUser(UUID.fromString(result.getString("idUser")));
                    return song;
                } else {
                    return null;
                }
            }
        }
    }
}
