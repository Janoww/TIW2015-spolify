package it.polimi.tiw.projects.dao;

import static org.junit.jupiter.api.Assertions.*;
import it.polimi.tiw.projects.beans.Album;
import it.polimi.tiw.projects.beans.Playlist;
import it.polimi.tiw.projects.beans.Song;
import it.polimi.tiw.projects.beans.User;
import it.polimi.tiw.projects.exceptions.DAOException;
import it.polimi.tiw.projects.exceptions.DAOException.DAOErrorType;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class PlaylistDAOTest {

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
    private static final String TEST_SONG_GENRE = "Test";
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

            // Create test album and song
            Album album = albumDAO.createAlbum(TEST_ALBUM_NAME, TEST_ALBUM_YEAR, TEST_ALBUM_ARTIST, testUserId);
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
            System.out.println("Database connection closed.");
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
        Integer playlistIdToFind = createdPlaylist.getIdPlaylist(); // Get ID from the returned object

        Playlist foundPlaylist = playlistDAO.findPlaylistById(playlistIdToFind, testUserId);

        assertNotNull(foundPlaylist, "Found playlist should not be null when searching by ID");
        assertEquals(playlistIdToFind, foundPlaylist.getIdPlaylist(), "Playlist ID should match");
        assertEquals(TEST_PLAYLIST_NAME, foundPlaylist.getName(), "Playlist name should match");
        assertNotNull(foundPlaylist.getBirthday(), "Playlist birthday should not be null");

        assertNotNull(foundPlaylist.getSongs(), "Playlist songs list should not be null");
        assertEquals(1, foundPlaylist.getSongs().size(), "Playlist should contain one song");
        assertEquals(createdSongId, foundPlaylist.getSongs().get(0), "Song ID in playlist should match");
    }

    @Test
    @Order(5)
    public void testCreatePlaylist_InvalidSong() throws SQLException { // Added throws SQLException
        List<Integer> invalidSongIds = List.of(-1);

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

    }

    @Test
    @Order(7)
    public void testAddSongToPlaylist() throws Exception {

    }

    @Test
    @Order(8)
    public void testRemoveSongFromPlaylist() throws Exception {

    }
}
