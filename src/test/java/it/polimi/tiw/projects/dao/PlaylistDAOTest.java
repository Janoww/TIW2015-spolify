package it.polimi.tiw.projects.dao;

import it.polimi.tiw.projects.beans.Album;
import it.polimi.tiw.projects.beans.Playlist;
import it.polimi.tiw.projects.beans.Song;
import it.polimi.tiw.projects.beans.User;
import it.polimi.tiw.projects.exceptions.DAOException;
import it.polimi.tiw.projects.exceptions.DAOException.DAOErrorType;
import it.polimi.tiw.projects.utils.Genre;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class PlaylistDAOTest {

    private static final Logger logger = LoggerFactory.getLogger(PlaylistDAOTest.class);
    // Database connection details
    private static final String DB_URL = "jdbc:mysql://localhost:3306/TIW2025";
    private static final String DB_USER = "tiw";
    private static final String DB_PASS = "TIW2025";
    // Test data
    private static final String TEST_USERNAME = "playlist_test_user_junit";
    private static final String TEST_PASSWORD = "password_playlist";
    private static final String TEST_NAME = "PlaylistJUnit";
    private static final String TEST_SURNAME = "Tester";
    private static final String TEST_PLAYLIST_NAME = "JUnit Test Playlist";
    private static final String TEST_PLAYLIST_NAME_DUPLICATE = "JUnit Test Playlist Duplicate";
    private static final String TEST_SONG_TITLE = "JUnit Test Song";
    private static final int TEST_SONG_YEAR = 2025;
    private static final Genre TEST_SONG_GENRE = Genre.POP;
    private static final String TEST_SONG_FILE = "/audio/test.mp3";
    private static final String TEST_ALBUM_NAME = "JUnit Test Album";
    private static final String TEST_ALBUM_ARTIST = "JUnit Artist";
    private static final int TEST_ALBUM_YEAR = 2024;
    // Second user for ownership tests
    private static final String TEST_USERNAME_2 = "playlist_test_user_junit_2_" + System.currentTimeMillis();
    private static final String TEST_PASSWORD_2 = "password_playlist_2";
    private static final String TEST_NAME_2 = "PlaylistJUnit2";
    private static final String TEST_SURNAME_2 = "Tester2";
    private static Connection connection;
    private static PlaylistDAO playlistDAO;
    private static UserDAO userDAO;
    private static SongDAO songDAO;
    private static AlbumDAO albumDAO;
    private static UUID testUserId;
    private static UUID testUserId2;
    private Integer createdSongIdUser2; // Song created by user 2

    private Integer createdPlaylistId;
    private Integer createdAlbumId;
    private Integer createdSongId; // Song created by user 1 (testUserId)

    @BeforeAll
    void setUpClass() throws SQLException {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
            connection.setAutoCommit(false);
            playlistDAO = new PlaylistDAO(connection);
            userDAO = new UserDAO(connection);
            songDAO = new SongDAO(connection);
            albumDAO = new AlbumDAO(connection);

            // Cleanup existing test data
            cleanupTestPlaylists();
            cleanupTestSongs();
            cleanupTestAlbums();
            cleanupTestUsers();
            connection.commit();

            // Create test user 1
            userDAO.createUser(TEST_USERNAME, TEST_PASSWORD, TEST_NAME, TEST_SURNAME);
            connection.commit();
            User testUser1 = userDAO.checkCredentials(TEST_USERNAME, TEST_PASSWORD);
            assertNotNull(testUser1, "Test user 1 could not be created or found.");
            testUserId = testUser1.getIdUser();
            logger.info("Test user 1 (testUserId) created with ID: {}", testUserId);

            // Create test user 2
            userDAO.createUser(TEST_USERNAME_2, TEST_PASSWORD_2, TEST_NAME_2, TEST_SURNAME_2);
            connection.commit();
            User testUser2 = userDAO.checkCredentials(TEST_USERNAME_2, TEST_PASSWORD_2);
            assertNotNull(testUser2, "Test user 2 could not be created or found.");
            testUserId2 = testUser2.getIdUser();
            logger.info("Test user 2 (testUserId2) created with ID: {}", testUserId2);

            // Create test album for user 1 (pass null for image)
            Album album = albumDAO.createAlbum(TEST_ALBUM_NAME, TEST_ALBUM_YEAR, TEST_ALBUM_ARTIST, null, testUserId);
            connection.commit();
            createdAlbumId = album.getIdAlbum();
            assertNotNull(createdAlbumId, "Created album ID for user 1 should not be null.");
            logger.info("Test album created with ID: {} for user 1", createdAlbumId);

            // Create test song for user 1
            Song songUser1 = songDAO.createSong(TEST_SONG_TITLE, createdAlbumId, TEST_SONG_YEAR, TEST_SONG_GENRE,
                    TEST_SONG_FILE, testUserId);
            connection.commit();
            createdSongId = songUser1.getIdSong();
            assertNotNull(createdSongId, "Created song ID for user 1 should not be null.");
            logger.info("Test song created with ID: {} for user 1", createdSongId);

            String songTitleUser2 = "JUnit Test Song User2 - " + System.currentTimeMillis();
            Song songUser2 = songDAO.createSong(songTitleUser2, createdAlbumId, TEST_SONG_YEAR, Genre.BLUES,
                    "/audio/test_user2.mp3", testUserId2);
            connection.commit();
            createdSongIdUser2 = songUser2.getIdSong();
            assertNotNull(createdSongIdUser2, "Created song ID for user 2 should not be null.");
            logger.info("Test song created with ID: {} for user 2", createdSongIdUser2);

        } catch (ClassNotFoundException | DAOException e) {
            throw new SQLException("Initialization failed: " + e.getMessage());
        }
    }

    @AfterAll
    void tearDownClass() throws SQLException {
        cleanupTestPlaylists();
        cleanupTestUsers();
        if (connection != null && !connection.isClosed()) {
            connection.commit(); // Commit final cleanup
            connection.close();
            logger.info("Database connection closed.");
        }
    }

    @BeforeEach
    void setUp() throws SQLException {
        connection.setAutoCommit(false);
        cleanupTestPlaylists();
    }

    @AfterEach
    void tearDown() throws SQLException {
        connection.rollback();
    }

    private void cleanupTestPlaylists() throws SQLException {
        String deleteSQL = "DELETE FROM playlist_metadata WHERE idUser = UUID_TO_BIN(?) OR idUser = UUID_TO_BIN(?)";
        try (PreparedStatement pStatement = connection.prepareStatement(deleteSQL)) {
            pStatement.setString(1, testUserId != null ? testUserId.toString() : UUID.randomUUID().toString());
            pStatement.setString(2, testUserId2 != null ? testUserId2.toString() : UUID.randomUUID().toString());
            pStatement.executeUpdate();
        }
    }

    private void cleanupTestSongs() throws SQLException {
        // Clean songs for user 1
        if (testUserId != null) {
            String deleteSQL = "DELETE FROM Song WHERE idUser = UUID_TO_BIN(?)";
            try (PreparedStatement pStatement = connection.prepareStatement(deleteSQL)) {
                pStatement.setString(1, testUserId.toString());
                pStatement.executeUpdate();
            }
        }
        // Clean songs for user 2
        if (testUserId2 != null) {
            String deleteSQL = "DELETE FROM Song WHERE idUser = UUID_TO_BIN(?)";
            try (PreparedStatement pStatement = connection.prepareStatement(deleteSQL)) {
                pStatement.setString(1, testUserId2.toString());
                pStatement.executeUpdate();
            }
        }
        // Also clean by specific IDs if they were set, as a fallback
        if (createdSongId != null) {
            String deleteSQL = "DELETE FROM Song WHERE idSong = ?";
            try (PreparedStatement pStatement = connection.prepareStatement(deleteSQL)) {
                pStatement.setInt(1, createdSongId);
                pStatement.executeUpdate();
            }
        }
        if (createdSongIdUser2 != null) {
            String deleteSQL = "DELETE FROM Song WHERE idSong = ?";
            try (PreparedStatement pStatement = connection.prepareStatement(deleteSQL)) {
                pStatement.setInt(1, createdSongIdUser2);
                pStatement.executeUpdate();
            }
        }
    }

    private void cleanupTestAlbums() throws SQLException {
        // Albums are currently only created by testUserId
        if (testUserId != null) {
            String deleteSQL = "DELETE FROM Album WHERE idUser = UUID_TO_BIN(?)";
            try (PreparedStatement pStatement = connection.prepareStatement(deleteSQL)) {
                pStatement.setString(1, testUserId.toString());
                pStatement.executeUpdate();
            }
        }
        if (createdAlbumId != null) {
            String deleteSQL = "DELETE FROM Album WHERE idAlbum = ?";
            try (PreparedStatement pStatement = connection.prepareStatement(deleteSQL)) {
                pStatement.setInt(1, createdAlbumId);
                pStatement.executeUpdate();
            }
        }
    }

    private void cleanupTestUsers() throws SQLException {
        String deleteSQL = "DELETE FROM User WHERE username = ? OR username = ?";
        try (PreparedStatement pStatement = connection.prepareStatement(deleteSQL)) {
            pStatement.setString(1, TEST_USERNAME);
            pStatement.setString(2, TEST_USERNAME_2);
            pStatement.executeUpdate();
        }
    }

    @Test
    @Order(1)
    void testCreatePlaylist_Success() throws Exception {
        List<Integer> songIds = new ArrayList<>();
        songIds.add(createdSongId);

        // Verify metadata - should NOT exist before commit
        try (PreparedStatement pStatement = connection.prepareStatement(
                "SELECT idPlaylist FROM playlist_metadata WHERE name = ? AND idUser = UUID_TO_BIN(?)")) {
            pStatement.setString(1, TEST_PLAYLIST_NAME);
            pStatement.setString(2, testUserId.toString());
            try (ResultSet rs = pStatement.executeQuery()) {
                assertFalse(rs.next(), "Playlist metadata should not exist before commit");
            }
        }

        // Verify content - should NOT exist before commit
        try (PreparedStatement pStatement = connection.prepareStatement(
                "SELECT pc.idSong FROM playlist_content pc JOIN playlist_metadata pm ON pc.idPlaylist = pm.idPlaylist WHERE pm.name = ? AND pm.idUser = UUID_TO_BIN(?)")) {
            pStatement.setString(1, TEST_PLAYLIST_NAME);
            pStatement.setString(2, testUserId.toString());
            try (ResultSet rs = pStatement.executeQuery()) {
                assertFalse(rs.next(), "Playlist content should not exist before commit");
            }
        }

        assertDoesNotThrow(() -> playlistDAO.createPlaylist(TEST_PLAYLIST_NAME, testUserId, songIds));

        connection.commit(); // Manually commit the transaction

        // Verify metadata - should exist after commit
        try (PreparedStatement pStatement = connection.prepareStatement(
                "SELECT idPlaylist, birthday FROM playlist_metadata WHERE name = ? AND idUser = UUID_TO_BIN(?)")) {
            pStatement.setString(1, TEST_PLAYLIST_NAME);
            pStatement.setString(2, testUserId.toString());
            try (ResultSet rs = pStatement.executeQuery()) {
                assertTrue(rs.next(), "Playlist metadata should exist after commit");
                assertNotNull(rs.getTimestamp("birthday"), "Birthday should not be null after commit");
                createdPlaylistId = rs.getInt("idPlaylist"); // Store the ID for later
                // use/verification
                assertFalse(rs.next(), "Should only be one playlist with this name for this user");
            }
        }
        assertNotNull(createdPlaylistId, "Created playlist ID should not be null");

        // Verify content - should exist after commit
        try (PreparedStatement pStatement = connection
                .prepareStatement("SELECT idSong FROM playlist_content WHERE idPlaylist = ?")) {
            pStatement.setInt(1, createdPlaylistId);
            try (ResultSet rs = pStatement.executeQuery()) {
                assertTrue(rs.next(), "Playlist content should exist after commit");
                assertEquals(createdSongId, rs.getInt("idSong"), "Song ID in playlist content should match");
                assertFalse(rs.next(), "Should only be one song in the playlist initially");
            }
        }
    }

    @Test
    @Order(2)
    void testCreatePlaylist_DuplicateName() throws Exception {
        List<Integer> songIds = new ArrayList<>();
        songIds.add(createdSongId);

        // First creation should succeed
        assertDoesNotThrow(() -> playlistDAO.createPlaylist(TEST_PLAYLIST_NAME_DUPLICATE, testUserId, songIds));

        connection.commit();

        // Second creation should fail
        DAOException exception = assertThrows(DAOException.class,
                () -> playlistDAO.createPlaylist(TEST_PLAYLIST_NAME_DUPLICATE, testUserId, songIds));

        connection.rollback(); // Rollback the failed transaction explicitly for clarity

        assertEquals(DAOErrorType.NAME_ALREADY_EXISTS, exception.getErrorType());
        assertTrue(exception.getMessage().contains(TEST_PLAYLIST_NAME_DUPLICATE));

        // Verify only one playlist exists with the duplicate name after the failed
        // attempt
        try (PreparedStatement pStatement = connection.prepareStatement(
                "SELECT COUNT(*) as count FROM playlist_metadata WHERE name = ? AND idUser = UUID_TO_BIN(?)")) {
            pStatement.setString(1, TEST_PLAYLIST_NAME_DUPLICATE);
            pStatement.setString(2, testUserId.toString());
            try (ResultSet rs = pStatement.executeQuery()) {
                assertTrue(rs.next(), "Should be able to query count");
                assertEquals(1, rs.getInt("count"), "Only one playlist with the duplicate name should exist");
            }
        }
    }

    @Test
    @Order(3)
    void testFindPlaylistsByUser() throws Exception {
        List<Integer> songIds = new ArrayList<>();
        songIds.add(createdSongId);
        assertDoesNotThrow(() -> playlistDAO.createPlaylist(TEST_PLAYLIST_NAME, testUserId, songIds));

        connection.commit();

        List<Playlist> playlists = assertDoesNotThrow(() -> playlistDAO.findPlaylistsByUser(testUserId));

        assertNotNull(playlists, "Playlist list should not be null");
        assertEquals(1, playlists.size(), "Should find exactly one playlist for the user");

        Playlist firstPlaylist = playlists.getFirst();
        assertNotNull(firstPlaylist, "First playlist object should not be null");
        assertEquals(TEST_PLAYLIST_NAME, firstPlaylist.getName(), "Playlist name should match the created one");
        assertNotNull(firstPlaylist.getSongs(), "Playlist songs should not be null");
        assertFalse(firstPlaylist.getSongs().isEmpty(), "Playlist songs should not be empty");
        assertEquals(createdSongId, firstPlaylist.getSongs().getFirst(), "Song ID in playlist should match");

        // Optional: Direct DB check to be absolutely sure
        try (PreparedStatement pStatement = connection.prepareStatement(
                "SELECT name, birthday FROM playlist_metadata WHERE idPlaylist = ? AND idUser = UUID_TO_BIN(?)")) {
            pStatement.setInt(1, firstPlaylist.getIdPlaylist());
            pStatement.setString(2, testUserId.toString());

            try (ResultSet rs = pStatement.executeQuery()) {
                assertTrue(rs.next(), "Playlist should exist in DB");
                assertEquals(TEST_PLAYLIST_NAME, rs.getString("name"));
                assertNotNull(rs.getTimestamp("birthday"));
                assertFalse(rs.next(), "Should only be one matching playlist in DB");
            }
        }
    }

    @Test
    @Order(4)
    void testFindPlaylistById() throws Exception {
        List<Integer> songIds = List.of(createdSongId);

        Playlist createdPlaylist = playlistDAO.createPlaylist(TEST_PLAYLIST_NAME, testUserId, songIds);
        // Commit is needed here in the test context, as createPlaylist might throw an
        // exception before its internal commit.
        // The DAO method handles its own commit on success.
        connection.commit();

        assertNotNull(createdPlaylist, "createPlaylist should return the created playlist object");
        // Check if the ID is set (primitive int cannot be null, but check if it's the
        // default 0 if not set)
        assertTrue(createdPlaylist.getIdPlaylist() > 0, "Created playlist must have a valid ID (> 0) from the DAO");
        int playlistIdToFind = createdPlaylist.getIdPlaylist(); // Get ID from the returned object

        // Successful find
        Playlist foundPlaylist = assertDoesNotThrow(() -> playlistDAO.findPlaylistById(playlistIdToFind, testUserId),
                "Finding an existing playlist by owner should not throw.");

        assertNotNull(foundPlaylist, "Found playlist should not be null");
        assertEquals(playlistIdToFind, foundPlaylist.getIdPlaylist(), "Playlist ID should match");
        assertEquals(TEST_PLAYLIST_NAME, foundPlaylist.getName(), "Playlist name should match");
        assertNotNull(foundPlaylist.getBirthday(), "Playlist birthday should not be null");

        assertNotNull(foundPlaylist.getSongs(), "Playlist songs list should not be null");
        assertEquals(1, foundPlaylist.getSongs().size(), "Playlist should contain one song");
        assertEquals(createdSongId, foundPlaylist.getSongs().getFirst(), "Song ID in playlist should match");

        // Test NOT_FOUND
        final int nonExistentPlaylistId = 99999;
        DAOException notFoundException = assertThrows(DAOException.class,
                () -> playlistDAO.findPlaylistById(nonExistentPlaylistId, testUserId),
                "Finding non-existent playlist should throw DAOException.");
        assertEquals(DAOErrorType.NOT_FOUND, notFoundException.getErrorType());

        // Test ACCESS_DENIED
        UUID otherUserId = UUID.randomUUID();
        DAOException accessDeniedException = assertThrows(DAOException.class,
                () -> playlistDAO.findPlaylistById(playlistIdToFind, otherUserId),
                "Finding playlist owned by another user should throw DAOException.");
        assertEquals(DAOErrorType.ACCESS_DENIED, accessDeniedException.getErrorType());
    }

    @Test
    @Order(5)
    void testCreatePlaylist_InvalidSong() throws SQLException {
        List<Integer> invalidSongIds = List.of(-1); // Assuming -1 is an invalid/non-existent song
        // ID

        // Expect NOT_FOUND because the song ID check in createPlaylist should fail
        DAOException exception = assertThrows(DAOException.class,
                () -> playlistDAO.createPlaylist("Invalid Song Playlist", testUserId, invalidSongIds));
        connection.rollback(); // Ensure transaction is rolled back after expected failure

        assertEquals(DAOErrorType.NOT_FOUND, exception.getErrorType());

        // Verify no playlist metadata was created
        try (PreparedStatement pStatement = connection.prepareStatement(
                "SELECT idPlaylist FROM playlist_metadata WHERE name = ? AND idUser = UUID_TO_BIN(?)")) {
            pStatement.setString(1, "Invalid Song Playlist");
            pStatement.setString(2, testUserId.toString());
            try (ResultSet rs = pStatement.executeQuery()) {
                assertFalse(rs.next(), "No playlist metadata should exist after failed creation");
            }
        }
    }

    @Test
    @Order(6)
    void testDeletePlaylist() throws Exception {
        // 1. Create a playlist to delete
        List<Integer> songIds = List.of(createdSongId);
        Playlist playlistToDelete = playlistDAO.createPlaylist("Playlist To Delete", testUserId, songIds);
        connection.commit();
        assertNotNull(playlistToDelete, "Playlist for deletion should be created");
        int playlistIdToDelete = playlistToDelete.getIdPlaylist();
        assertTrue(playlistIdToDelete > 0, "Playlist ID for deletion must be valid");

        // 2. Verify it exists before deletion
        Playlist foundBeforeDelete = playlistDAO.findPlaylistById(playlistIdToDelete, testUserId);
        assertNotNull(foundBeforeDelete, "Playlist should be findable before deletion");

        // 3. Delete the playlist - Now returns void
        assertDoesNotThrow(() -> playlistDAO.deletePlaylist(playlistIdToDelete, testUserId),
                "Successful delete should not throw an exception.");
        connection.commit();

        // 4. Verify it's gone by expecting NOT_FOUND when trying to find it
        DAOException findException = assertThrows(DAOException.class,
                () -> playlistDAO.findPlaylistById(playlistIdToDelete, testUserId),
                "Finding deleted playlist should throw NOT_FOUND.");
        assertEquals(DAOErrorType.NOT_FOUND, findException.getErrorType());

        // 5. Verify associated content is gone (due to ON DELETE CASCADE)
        try (PreparedStatement pStatement = connection
                .prepareStatement("SELECT COUNT(*) as count FROM playlist_content WHERE idPlaylist = ?")) {
            pStatement.setInt(1, playlistIdToDelete);
            try (ResultSet rs = pStatement.executeQuery()) {
                assertTrue(rs.next());
                assertEquals(0, rs.getInt("count"), "Playlist content should be deleted by cascade");
            }
        }

        // 6. Test deleting a non-existent playlist - Should throw NOT_FOUND
        final int nonExistentPlaylistId = 99999;
        DAOException notFoundException = assertThrows(DAOException.class,
                () -> playlistDAO.deletePlaylist(nonExistentPlaylistId, testUserId),
                "Deleting non-existent playlist should throw NOT_FOUND.");
        assertEquals(DAOErrorType.NOT_FOUND, notFoundException.getErrorType());
        connection.rollback(); // Rollback after expected exception

        // 7. Test deleting by non-owner - Should throw ACCESS_DENIED
        // Recreate playlist first
        playlistToDelete = playlistDAO.createPlaylist("Playlist To Delete Again", testUserId, songIds);
        connection.commit();
        final int finalPlaylistIdToDelete = playlistToDelete.getIdPlaylist(); // Use final variable
        // for lambda
        UUID otherUserId = UUID.randomUUID();
        DAOException accessDeniedException = assertThrows(DAOException.class,
                () -> playlistDAO.deletePlaylist(finalPlaylistIdToDelete, otherUserId),
                "Deleting playlist by non-owner should throw ACCESS_DENIED.");
        assertEquals(DAOErrorType.ACCESS_DENIED, accessDeniedException.getErrorType());
        connection.rollback(); // Rollback after expected exception
        // Verify it still exists
        assertNotNull(playlistDAO.findPlaylistById(finalPlaylistIdToDelete, testUserId),
                "Playlist should still exist after failed delete attempt by non-owner.");

    }

    @Test
    @Order(7)
    void testAddSongToPlaylist() throws Exception {
        // 1. Create a playlist (initially empty for this test)
        Playlist playlist = playlistDAO.createPlaylist("Playlist For Adding Songs", testUserId, new ArrayList<>());
        connection.commit();
        assertNotNull(playlist, "Playlist for adding songs should be created");
        int playlistId = playlist.getIdPlaylist();
        assertTrue(playlistId > 0, "Playlist ID must be valid");

        // 2. Create a second song to add
        Song secondSong = songDAO.createSong("Second JUnit Song", createdAlbumId, TEST_SONG_YEAR, TEST_SONG_GENRE,
                "/audio/second_test.mp3", testUserId);
        connection.commit();
        assertNotNull(secondSong, "Second song should be created");
        int secondSongId = secondSong.getIdSong();
        assertTrue(secondSongId > 0, "Second song ID must be valid");

        // 3. Add the first song (created in @BeforeAll) - Now returns void
        assertDoesNotThrow(() -> playlistDAO.addSongToPlaylist(playlistId, testUserId, createdSongId),
                "Adding first song should not throw.");
        connection.commit();

        // 4. Verify the first song is there
        Playlist playlistAfterFirstAdd = playlistDAO.findPlaylistById(playlistId, testUserId);
        assertNotNull(playlistAfterFirstAdd);
        assertEquals(1, playlistAfterFirstAdd.getSongs().size());
        assertTrue(playlistAfterFirstAdd.getSongs().contains(createdSongId));

        // 5. Add the second song - Now returns void
        assertDoesNotThrow(() -> playlistDAO.addSongToPlaylist(playlistId, testUserId, secondSongId),
                "Adding second song should not throw.");
        connection.commit();

        // 6. Verify both songs are there
        Playlist playlistAfterSecondAdd = playlistDAO.findPlaylistById(playlistId, testUserId);
        assertNotNull(playlistAfterSecondAdd);
        assertEquals(2, playlistAfterSecondAdd.getSongs().size());
        assertTrue(playlistAfterSecondAdd.getSongs().contains(createdSongId));
        assertTrue(playlistAfterSecondAdd.getSongs().contains(secondSongId));

        // 7. Test adding a duplicate song - Should throw DUPLICATE_ENTRY
        DAOException duplicateException = assertThrows(DAOException.class,
                () -> playlistDAO.addSongToPlaylist(playlistId, testUserId, createdSongId),
                "Adding a duplicate song should throw DUPLICATE_ENTRY.");
        assertEquals(DAOErrorType.DUPLICATE_ENTRY, duplicateException.getErrorType());
        connection.rollback(); // Rollback after expected exception
        Playlist playlistAfterDuplicateAdd = playlistDAO.findPlaylistById(playlistId, testUserId);
        assertEquals(2, playlistAfterDuplicateAdd.getSongs().size(),
                "Playlist size should not change after duplicate add attempt");

        // 8. Test adding a non-existent song - Should throw NOT_FOUND
        final int nonExistentSongId = 99999;
        DAOException songNotFoundException = assertThrows(DAOException.class,
                () -> playlistDAO.addSongToPlaylist(playlistId, testUserId, nonExistentSongId),
                "Adding non-existent song should throw NOT_FOUND.");
        assertEquals(DAOErrorType.NOT_FOUND, songNotFoundException.getErrorType());
        assertTrue(songNotFoundException.getMessage().contains("Song with ID " + nonExistentSongId + " not found"));
        connection.rollback(); // Rollback after expected exception

        // 9. Test adding to a non-existent playlist - Should throw NOT_FOUND
        final int nonExistentPlaylistId = 88888;
        DAOException playlistNotFoundException = assertThrows(DAOException.class,
                () -> playlistDAO.addSongToPlaylist(nonExistentPlaylistId, testUserId, createdSongId),
                "Adding song to non-existent playlist should throw NOT_FOUND.");
        assertEquals(DAOErrorType.NOT_FOUND, playlistNotFoundException.getErrorType());
        assertTrue(playlistNotFoundException.getMessage()
                .contains("Playlist with ID " + nonExistentPlaylistId + " not found"));
        connection.rollback(); // Rollback after expected exception

        // 10. Test adding by non-owner - Should throw ACCESS_DENIED
        UUID otherUserId = UUID.randomUUID();
        DAOException accessDeniedException = assertThrows(DAOException.class,
                () -> playlistDAO.addSongToPlaylist(playlistId, otherUserId, createdSongId),
                "Adding song by non-owner should throw ACCESS_DENIED.");
        assertEquals(DAOErrorType.ACCESS_DENIED, accessDeniedException.getErrorType());
        connection.rollback(); // Rollback after expected exception
    }

    @Test
    @Order(8)
    void testRemoveSongFromPlaylist() throws Exception {
        // 1. Create a second song
        Song secondSong = songDAO.createSong("Second JUnit Song For Removal", createdAlbumId, TEST_SONG_YEAR,
                TEST_SONG_GENRE, "/audio/second_removal_test.mp3", testUserId);
        connection.commit();
        assertNotNull(secondSong, "Second song for removal should be created");
        int secondSongId = secondSong.getIdSong();
        assertTrue(secondSongId > 0, "Second song ID for removal must be valid");

        // 2. Create a playlist with both songs
        List<Integer> initialSongIds = List.of(createdSongId, secondSongId);
        Playlist playlist = playlistDAO.createPlaylist("Playlist For Removing Songs", testUserId, initialSongIds);
        connection.commit();
        assertNotNull(playlist, "Playlist for removing songs should be created");
        int playlistId = playlist.getIdPlaylist();
        assertTrue(playlistId > 0, "Playlist ID must be valid");

        // 3. Verify both songs are initially present
        Playlist playlistBeforeRemoval = playlistDAO.findPlaylistById(playlistId, testUserId);
        assertNotNull(playlistBeforeRemoval);
        assertEquals(2, playlistBeforeRemoval.getSongs().size());
        assertTrue(playlistBeforeRemoval.getSongs().contains(createdSongId));
        assertTrue(playlistBeforeRemoval.getSongs().contains(secondSongId));

        // 4. Remove the second song - Returns boolean
        boolean removedSecond = assertDoesNotThrow(
                () -> playlistDAO.removeSongFromPlaylist(playlistId, testUserId, secondSongId),
                "Removing existing song should not throw.");
        connection.commit();
        assertTrue(removedSecond, "Should return true when removing an existing song");

        // 5. Verify only the first song remains
        Playlist playlistAfterRemoval = playlistDAO.findPlaylistById(playlistId, testUserId);
        assertNotNull(playlistAfterRemoval);
        assertEquals(1, playlistAfterRemoval.getSongs().size());
        assertTrue(playlistAfterRemoval.getSongs().contains(createdSongId));
        assertFalse(playlistAfterRemoval.getSongs().contains(secondSongId));

        // 6. Test removing a song not in the playlist - Returns boolean
        boolean removedNonPresent = assertDoesNotThrow(
                () -> playlistDAO.removeSongFromPlaylist(playlistId, testUserId, secondSongId), // Try
                // removing
                // again
                "Removing non-present song should not throw.");
        connection.commit(); // Commit even if no change expected
        assertFalse(removedNonPresent, "Removing a song not present should return false");
        Playlist playlistAfterRemovingNonPresent = playlistDAO.findPlaylistById(playlistId, testUserId);
        assertEquals(1, playlistAfterRemovingNonPresent.getSongs().size(),
                "Playlist size should not change after removing non-present song");

        // 7. Test removing from a non-existent playlist - Should throw NOT_FOUND
        final int nonExistentPlaylistId = 88888;
        DAOException notFoundException = assertThrows(DAOException.class,
                () -> playlistDAO.removeSongFromPlaylist(nonExistentPlaylistId, testUserId, createdSongId),
                "Removing song from non-existent playlist should throw NOT_FOUND.");
        assertEquals(DAOErrorType.NOT_FOUND, notFoundException.getErrorType());
        connection.rollback(); // Rollback after expected exception

        // 8. Test removing by non-owner - Should throw ACCESS_DENIED
        UUID otherUserId = UUID.randomUUID();
        DAOException accessDeniedException = assertThrows(DAOException.class,
                () -> playlistDAO.removeSongFromPlaylist(playlistId, otherUserId, createdSongId),
                "Removing song by non-owner should throw ACCESS_DENIED.");
        assertEquals(DAOErrorType.ACCESS_DENIED, accessDeniedException.getErrorType());
        connection.rollback(); // Rollback after expected exception
        // Verify song is still there
        Playlist playlistAfterFailedRemove = playlistDAO.findPlaylistById(playlistId, testUserId);
        assertTrue(playlistAfterFailedRemove.getSongs().contains(createdSongId),
                "Song should still be in playlist after failed removal attempt by non-owner.");
    }

    @Test
    @Order(9)
    void testCreatePlaylist_WithNullSongIdsList() throws Exception {
        assertNotNull(testUserId, "TestUser ID must be set.");
        String playlistName = "Playlist With Null Song List";

        Playlist playlist = assertDoesNotThrow(() -> playlistDAO.createPlaylist(playlistName, testUserId, null),
                "Creating playlist with null song ID list should not throw.");
        connection.commit();

        assertNotNull(playlist, "Playlist object should not be null.");
        assertTrue(playlist.getIdPlaylist() > 0, "Playlist ID should be positive.");
        assertEquals(playlistName, playlist.getName());
        assertNotNull(playlist.getSongs(), "Songs list should not be null.");
        assertTrue(playlist.getSongs().isEmpty(), "Songs list should be empty for null input.");

        // Verify directly in DB
        Playlist foundPlaylist = playlistDAO.findPlaylistById(playlist.getIdPlaylist(), testUserId);
        assertNotNull(foundPlaylist);
        assertTrue(foundPlaylist.getSongs().isEmpty());
    }

    @Test
    @Order(10)
    void testCreatePlaylist_WithEmptySongIdsList() throws Exception {
        assertNotNull(testUserId, "TestUser ID must be set.");
        String playlistName = "Playlist With Empty Song List";
        List<Integer> emptySongIds = new ArrayList<>();

        Playlist playlist = assertDoesNotThrow(() -> playlistDAO.createPlaylist(playlistName, testUserId, emptySongIds),
                "Creating playlist with empty song ID list should not throw.");
        connection.commit();

        assertNotNull(playlist, "Playlist object should not be null.");
        assertTrue(playlist.getIdPlaylist() > 0, "Playlist ID should be positive.");
        assertEquals(playlistName, playlist.getName());
        assertNotNull(playlist.getSongs(), "Songs list should not be null.");
        assertTrue(playlist.getSongs().isEmpty(), "Songs list should be empty for empty input.");

        // Verify directly in DB
        Playlist foundPlaylist = playlistDAO.findPlaylistById(playlist.getIdPlaylist(), testUserId);
        assertNotNull(foundPlaylist);
        assertTrue(foundPlaylist.getSongs().isEmpty());
    }

    @Test
    @Order(11)
    void testCreatePlaylist_WithNullSongIdInList() throws SQLException {
        assertNotNull(testUserId, "TestUser ID must be set.");
        assertNotNull(createdSongId, "CreatedSongId for user 1 must be set.");
        String playlistName = "Playlist With Null SongId In List";
        List<Integer> songIdsWithNull = new ArrayList<>();
        songIdsWithNull.add(createdSongId);
        songIdsWithNull.add(null); // Add a null song ID

        DAOException exception = assertThrows(DAOException.class,
                () -> playlistDAO.createPlaylist(playlistName, testUserId, songIdsWithNull),
                "Creating playlist with a null song ID in the list should throw DAOException.");
        connection.rollback();

        assertEquals(DAOErrorType.CONSTRAINT_VIOLATION, exception.getErrorType());
        assertTrue(exception.getMessage().contains("Playlist cannot contain null song IDs."));

        // Verify playlist was not created
        try (PreparedStatement pStatement = connection.prepareStatement(
                "SELECT idPlaylist FROM playlist_metadata WHERE name = ? AND idUser = UUID_TO_BIN(?)")) {
            pStatement.setString(1, playlistName);
            pStatement.setString(2, testUserId.toString());
            try (ResultSet rs = pStatement.executeQuery()) {
                assertFalse(rs.next(), "Playlist metadata should not exist after failed creation with null song ID.");
            }
        }
    }

    @Test
    @Order(12)
    void testCreatePlaylist_WithDuplicateSongIdInList() throws SQLException {
        assertNotNull(testUserId, "TestUser ID must be set.");
        assertNotNull(createdSongId, "CreatedSongId for user 1 must be set.");
        String playlistName = "Playlist With Duplicate SongId In List";
        List<Integer> songIdsWithDuplicate = List.of(createdSongId, createdSongId);

        // This should throw DUPLICATE_ENTRY due to the unique constraint on
        // (idPlaylist, idSong)
        // when batch inserting into playlist_content.
        DAOException exception = assertThrows(DAOException.class,
                () -> playlistDAO.createPlaylist(playlistName, testUserId, songIdsWithDuplicate),
                "Creating playlist with duplicate song IDs in the list should throw DAOException.");
        connection.rollback();

        assertEquals(DAOErrorType.DUPLICATE_ENTRY, exception.getErrorType());
        assertTrue(exception.getMessage().contains("Duplicate song ID found in the input list"),
                "Exception message mismatch for duplicate song in list");
    }

    @Test
    @Order(13)
    void testAddSongToPlaylist_SongNotOwnedByUser() throws Exception {
        assertNotNull(testUserId, "TestUser ID 1 must be set.");
        assertNotNull(testUserId2, "TestUser ID 2 must be set.");
        assertNotNull(createdSongIdUser2, "createdSongIdUser2 (song by user 2) must be set.");

        // Create a playlist owned by user 1
        String playlistName = "User1 Playlist For Song Ownership Test";
        Playlist playlistUser1 = playlistDAO.createPlaylist(playlistName, testUserId, new ArrayList<>());
        connection.commit();
        assertNotNull(playlistUser1, "Playlist for user 1 should be created.");
        int playlistIdUser1 = playlistUser1.getIdPlaylist();

        // Attempt to add song owned by user 2 (createdSongIdUser2) to user 1's playlist
        // This should fail because checkSongExistsAndOwnership checks ownership against
        // the playlist owner (testUserId)
        DAOException exception = assertThrows(DAOException.class,
                () -> playlistDAO.addSongToPlaylist(playlistIdUser1, testUserId, createdSongIdUser2),
                "Adding a song not owned by the playlist owner should throw DAOException.");
        connection.rollback();

        // Expect NOT_FOUND because checkSongExistsAndOwnership (called with testUserId)
        // won't find createdSongIdUser2 for testUserId
        assertEquals(DAOErrorType.NOT_FOUND, exception.getErrorType());
        assertTrue(
                exception.getMessage()
                        .contains("Song with ID " + createdSongIdUser2 + " not found or not accessible to this user."),
                "Exception message should indicate song not found or not accessible to the playlist owner.");

        // Verify song was not added
        Playlist playlistAfterAttempt = playlistDAO.findPlaylistById(playlistIdUser1, testUserId);
        assertNotNull(playlistAfterAttempt);
        assertTrue(playlistAfterAttempt.getSongs().isEmpty(), "Playlist should remain empty.");
    }

    @Test
    @Order(14)
    void testFindPlaylistsByUser_NoPlaylists() {
        assertNotNull(testUserId2, "TestUser ID 2 (who has no playlists yet) must be set.");

        List<Playlist> playlists = assertDoesNotThrow(() -> playlistDAO.findPlaylistsByUser(testUserId2),
                "Finding playlists for a user with no playlists should not throw.");

        assertNotNull(playlists, "Playlist list should not be null.");
        assertTrue(playlists.isEmpty(), "Should find no playlists for a user who hasn't created any.");
    }
}
