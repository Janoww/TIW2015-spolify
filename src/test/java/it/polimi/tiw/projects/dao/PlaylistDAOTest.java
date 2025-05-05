package it.polimi.tiw.projects.dao;

import static org.junit.jupiter.api.Assertions.*;
import it.polimi.tiw.projects.beans.Album;
import it.polimi.tiw.projects.beans.Playlist;
import it.polimi.tiw.projects.beans.Song;
import it.polimi.tiw.projects.beans.User;
import it.polimi.tiw.projects.exceptions.DAOException;
import it.polimi.tiw.projects.exceptions.DAOException.DAOErrorType;
import it.polimi.tiw.projects.utils.Genre;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class PlaylistDAOTest {

    private static final Logger logger = LoggerFactory.getLogger(PlaylistDAOTest.class);

    private static Connection connection;
    private static PlaylistDAO playlistDAO;
    private static UserDAO userDAO;
    private static SongDAO songDAO;
    private static AlbumDAO albumDAO;

    // Database connection details
    private static final String DB_URL = "jdbc:mysql://localhost:3306/TIW2025";
    private static final String DB_USER = "tiw";
    private static final String DB_PASS = "TIW2025";

    // Test data
    private static final String TEST_USERNAME = "playlist_test_user_junit";
    private static final String TEST_PASSWORD = "password_playlist";
    private static final String TEST_NAME = "PlaylistJUnit";
    private static final String TEST_SURNAME = "Tester";
    private static UUID testUserId;

    private static final String TEST_PLAYLIST_NAME = "JUnit Test Playlist";
    private static final String TEST_PLAYLIST_NAME_DUPLICATE = "JUnit Test Playlist Duplicate";
    private static final String TEST_SONG_TITLE = "JUnit Test Song";
    private static final int TEST_SONG_YEAR = 2025;
    private static final Genre TEST_SONG_GENRE = Genre.POP;
    private static final String TEST_SONG_FILE = "/audio/test.mp3";
    private static final String TEST_ALBUM_NAME = "JUnit Test Album";
    private static final String TEST_ALBUM_ARTIST = "JUnit Artist";
    private static final int TEST_ALBUM_YEAR = 2024;

    private Integer createdPlaylistId;
    private Integer createdAlbumId;
    private Integer createdSongId;

    @BeforeAll
    public void setUpClass() throws SQLException {
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
            cleanupTestUser();
            connection.commit();

            // Create test user
            userDAO.createUser(TEST_USERNAME, TEST_PASSWORD, TEST_NAME, TEST_SURNAME);
            connection.commit();
            User testUser = userDAO.checkCredentials(TEST_USERNAME, TEST_PASSWORD);
            testUserId = testUser.getIdUser();

            // Create test album and song (pass null for image)
            Album album = albumDAO.createAlbum(TEST_ALBUM_NAME, TEST_ALBUM_YEAR, TEST_ALBUM_ARTIST, null, testUserId);
            connection.commit();
            createdAlbumId = album.getIdAlbum();

            Song song = songDAO.createSong(TEST_SONG_TITLE, createdAlbumId, TEST_SONG_YEAR, TEST_SONG_GENRE,
                    TEST_SONG_FILE, testUserId);
            connection.commit();
            createdSongId = song.getIdSong();

        } catch (ClassNotFoundException | DAOException e) {
            throw new SQLException("Initialization failed: " + e.getMessage());
        }
    }

    @AfterAll
    public void tearDownClass() throws SQLException {
        cleanupTestPlaylists();
        cleanupTestUser();
        if (connection != null && !connection.isClosed()) {
            connection.commit(); // Commit final cleanup
            connection.close();
            logger.info("Database connection closed.");
        }
    }

    @BeforeEach
    public void setUp() throws SQLException {
        connection.setAutoCommit(false);
        cleanupTestPlaylists();
    }

    @AfterEach
    public void tearDown() throws SQLException {
        connection.rollback();
    }

    private void cleanupTestPlaylists() throws SQLException {
        if (testUserId != null) {
            String deleteSQL = "DELETE FROM playlist_metadata WHERE idUser = UUID_TO_BIN(?)";
            try (PreparedStatement pStatement = connection.prepareStatement(deleteSQL)) {
                pStatement.setString(1, testUserId.toString());
                pStatement.executeUpdate();
            }
        }
    }

    private void cleanupTestSongs() throws SQLException {
        if (testUserId != null) {
            String deleteSQL = "DELETE FROM Song WHERE idUser = UUID_TO_BIN(?)";
            try (PreparedStatement pStatement = connection.prepareStatement(deleteSQL)) {
                pStatement.setString(1, testUserId.toString());
                pStatement.executeUpdate();
            }
        }
    }

    private void cleanupTestAlbums() throws SQLException {
        if (testUserId != null) {
            String deleteSQL = "DELETE FROM Album WHERE idUser = UUID_TO_BIN(?)";
            try (PreparedStatement pStatement = connection.prepareStatement(deleteSQL)) {
                pStatement.setString(1, testUserId.toString());
                pStatement.executeUpdate();
            }
        }
    }

    private void cleanupTestUser() throws SQLException {
        String deleteSQL = "DELETE FROM User WHERE username = ?";
        try (PreparedStatement pStatement = connection.prepareStatement(deleteSQL)) {
            pStatement.setString(1, TEST_USERNAME);
            pStatement.executeUpdate();
        }
    }

    @Test
    @Order(1)
    public void testCreatePlaylist_Success() throws Exception {
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
                createdPlaylistId = rs.getInt("idPlaylist"); // Store the ID for later use/verification
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
    public void testCreatePlaylist_DuplicateName() throws Exception {
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
    public void testFindPlaylistsByUser() throws Exception {
        List<Integer> songIds = new ArrayList<>();
        songIds.add(createdSongId);
        assertDoesNotThrow(() -> playlistDAO.createPlaylist(TEST_PLAYLIST_NAME, testUserId, songIds));

        connection.commit();

        List<Integer> playlists = assertDoesNotThrow(() -> playlistDAO.findPlaylistIdsByUser(testUserId));

        assertNotNull(playlists, "Playlist list should not be null");
        assertEquals(1, playlists.size(), "Should find exactly one playlist for the user");

        // Optional: Direct DB check to be absolutely sure
        try (PreparedStatement pStatement = connection.prepareStatement(
                "SELECT name, birthday FROM playlist_metadata WHERE idPlaylist = ? AND idUser = UUID_TO_BIN(?)")) {
            pStatement.setInt(1, playlists.get(0));
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
    public void testFindPlaylistById() throws Exception {
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
        assertEquals(createdSongId, foundPlaylist.getSongs().get(0), "Song ID in playlist should match");

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
    public void testCreatePlaylist_InvalidSong() throws SQLException {
        List<Integer> invalidSongIds = List.of(-1); // Assuming -1 is an invalid/non-existent song ID

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
    public void testDeletePlaylist() throws Exception {
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
        final int finalPlaylistIdToDelete = playlistToDelete.getIdPlaylist(); // Use final variable for lambda
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
    public void testAddSongToPlaylist() throws Exception {
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
    public void testRemoveSongFromPlaylist() throws Exception {
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
                () -> playlistDAO.removeSongFromPlaylist(playlistId, testUserId, secondSongId), // Try removing again
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
}
