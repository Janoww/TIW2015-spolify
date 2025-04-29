package it.polimi.tiw.projects.dao;

import it.polimi.tiw.projects.beans.Album;
import it.polimi.tiw.projects.beans.Album;
import it.polimi.tiw.projects.beans.Song;
import it.polimi.tiw.projects.beans.User;
import it.polimi.tiw.projects.exceptions.DAOException;
// import it.polimi.tiw.projects.utils.SongRegistry; // Removed registry import

import org.junit.jupiter.api.*;

import java.sql.*;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class SongDAOTest {

	private static Connection connection;
	private static SongDAO songDAO;
	private static UserDAO userDAO; // To manage a test user if needed
	private static AlbumDAO albumDAO; // To manage a test album

	// Database connection details
	private static final String DB_URL = "jdbc:mysql://localhost:3306/TIW2025";
	private static final String DB_USER = "tiw";
	private static final String DB_PASS = "TIW2025";

	// Test Data - Use unique values
	private static final String TEST_SONG_TITLE_1 = "JUnit Test Song 1 - " + System.currentTimeMillis();
	private static final String TEST_SONG_TITLE_2 = "JUnit Test Song 2 - " + System.currentTimeMillis();
	private static final int TEST_SONG_YEAR = 2025;
	private static final String TEST_GENRE = "TestGenre";
	private static final String TEST_AUDIO_FILE_1 = "/audio/junit_test1.mp3";
	private static final String TEST_AUDIO_FILE_2 = "/audio/junit_test2.mp3";
	private static UUID testUserId; // Will be generated

	// Test User details (needed for song creation)
	private static final String TEST_USERNAME = "song_test_user_junit";
	private static final String TEST_PASSWORD = "password_song";
	private static final String TEST_NAME = "SongJUnit";
	private static final String TEST_SURNAME = "Tester";

	// Test Album details
	private static final String TEST_ALBUM_TITLE = "JUnit Test Album - " + System.currentTimeMillis();
	private static final String TEST_ALBUM_ARTIST = "JUnit Artist";
	private static final int TEST_ALBUM_YEAR = 2024;
	private static Integer testAlbumId = null; // Will be generated

	private Integer createdSongId1 = null; // Store IDs of created songs for cleanup/verification
	private Integer createdSongId2 = null;

	@BeforeAll
	void setUpClass() throws SQLException, DAOException {
		try {
			Class.forName("com.mysql.cj.jdbc.Driver");
			connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
			connection.setAutoCommit(false); // Manage transactions manually
			songDAO = new SongDAO(connection);
			userDAO = new UserDAO(connection); // Initialize UserDAO for test user setup
			albumDAO = new AlbumDAO(connection); // Initialize AlbumDAO
			System.out.println("Database connection established for SongDAOTest.");

			// Initial cleanup of potential leftover test data
			cleanupTestSongs();
			cleanupTestAlbum(); // Clean up test album first due to FK
			cleanupTestUser(); // Also clean up the test user
			connection.commit();

			// Create the test user required for creating songs/albums
			userDAO.createUser(TEST_USERNAME, TEST_PASSWORD, TEST_NAME, TEST_SURNAME);
			connection.commit(); // Commit user creation
			User testUser = userDAO.checkCredentials(TEST_USERNAME, TEST_PASSWORD);
			assertNotNull(testUser, "Test user could not be created or found.");
			testUserId = testUser.getIdUser();
			System.out.println("Test user created with ID: " + testUserId);

			// Create the test album required for creating songs
			Album testAlbum = albumDAO.createAlbum(TEST_ALBUM_TITLE, TEST_ALBUM_YEAR, TEST_ALBUM_ARTIST, testUserId);
			connection.commit(); // Commit album creation
			assertNotNull(testAlbum, "Test album could not be created.");
			testAlbumId = testAlbum.getIdAlbum();
			assertNotNull(testAlbumId, "Test album ID is null after creation.");
			System.out.println("Test album created with ID: " + testAlbumId);

		} catch (ClassNotFoundException e) {
			System.err.println("MySQL JDBC Driver not found: " + e.getMessage());
			throw new SQLException("MySQL JDBC Driver not found.", e);
		} catch (SQLException e) {
			System.err.println("Failed to connect to the database '" + DB_URL + "': " + e.getMessage());
			throw e;
		} catch (DAOException e) {
			System.err.println("DAOException during test user setup: " + e.getMessage());
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
				cleanupTestSongs(); // Songs depend on Album and User
				cleanupTestAlbum(); // Album depends on User
				cleanupTestUser(); // User is independent
				connection.commit(); // Commit final cleanup
			} catch (SQLException e) {
				System.err.println("Error during final cleanup: " + e.getMessage());
				connection.rollback(); // Attempt rollback on cleanup error
			} finally {
				connection.close();
				System.out.println("Database connection closed for SongDAOTest.");
			}
		}
	}

	@BeforeEach
	void setUp() throws SQLException {
		// Clean songs before each test, commit the cleanup
		cleanupTestSongs();
		connection.commit();
		createdSongId1 = null; // Reset stored IDs
		createdSongId2 = null;
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
		// Robust cleanup: Delete any songs matching the test title pattern
		String deleteSQL = "DELETE FROM Song WHERE title LIKE ?";
		try (PreparedStatement pStatement = connection.prepareStatement(deleteSQL)) {
			pStatement.setString(1, "JUnit Test Song %"); // Pattern matching
			pStatement.executeUpdate();
		}
	}

	private void deleteSongById(int songId) throws SQLException {
		String deleteSQL = "DELETE FROM Song WHERE idSong = ?";
		try (PreparedStatement pStatement = connection.prepareStatement(deleteSQL)) {
			pStatement.setInt(1, songId);
			pStatement.executeUpdate();
		}
	}

	// Helper method to delete the test user
	private void cleanupTestUser() throws SQLException {
		String deleteSQL = "DELETE FROM User WHERE username = ?";
		try (PreparedStatement pStatement = connection.prepareStatement(deleteSQL)) {
			pStatement.setString(1, TEST_USERNAME);
			pStatement.executeUpdate();
		}
	}

	// Helper method to delete the test album
	private void cleanupTestAlbum() throws SQLException {
		if (testAlbumId != null) {
			deleteAlbumById(testAlbumId);
			testAlbumId = null; // Reset after deletion attempt
		}
		// Robust cleanup: Delete any albums matching the test name pattern
		String deleteSQL = "DELETE FROM Album WHERE name = ?";
		try (PreparedStatement pStatement = connection.prepareStatement(deleteSQL)) {
			pStatement.setString(1, TEST_ALBUM_TITLE); // TEST_ALBUM_TITLE holds the name used for creation
			pStatement.executeUpdate();
		}
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
	void testCreateSong_Success() throws DAOException, SQLException {
		assertNotNull(testAlbumId, "Test Album ID must be set before creating a song.");
		Song createdSong = assertDoesNotThrow(() -> songDAO.createSong(TEST_SONG_TITLE_1, testAlbumId, TEST_SONG_YEAR,
				TEST_GENRE, TEST_AUDIO_FILE_1, testUserId));

		assertNotNull(createdSong, "Created song object should not be null.");
		assertTrue(createdSong.getIdSong() > 0, "Created song ID should be positive.");
		createdSongId1 = createdSong.getIdSong(); // Store ID for cleanup/verification

		assertEquals(TEST_SONG_TITLE_1, createdSong.getTitle());
		assertEquals(testAlbumId, createdSong.getIdAlbum()); // Use dynamic album ID
		assertEquals(TEST_SONG_YEAR, createdSong.getYear());
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
		Song song1 = songDAO.createSong(TEST_SONG_TITLE_1, testAlbumId, TEST_SONG_YEAR, TEST_GENRE, TEST_AUDIO_FILE_1,
				testUserId);
		Song song2 = songDAO.createSong(TEST_SONG_TITLE_2, testAlbumId, TEST_SONG_YEAR + 1, TEST_GENRE,
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
	void testFindSongsByUser_Empty() throws DAOException {
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
		Song song1 = songDAO.createSong(TEST_SONG_TITLE_1, testAlbumId, TEST_SONG_YEAR, TEST_GENRE, TEST_AUDIO_FILE_1,
				testUserId);
		Song song2 = songDAO.createSong(TEST_SONG_TITLE_2, testAlbumId, TEST_SONG_YEAR + 1, TEST_GENRE,
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
	void testFindAllSongs_Empty() throws DAOException, SQLException {
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
		Song songToDelete = songDAO.createSong(TEST_SONG_TITLE_1, testAlbumId, TEST_SONG_YEAR, TEST_GENRE,
				TEST_AUDIO_FILE_1, testUserId);
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
			songDAO.createSong(TEST_SONG_TITLE_1, nonExistentAlbumId, TEST_SONG_YEAR, TEST_GENRE, TEST_AUDIO_FILE_1,
					testUserId);
			// Rollback will happen in @AfterEach
		}, "Creating a song with a non-existent album ID should throw DAOException.");

		assertEquals(DAOException.DAOErrorType.NOT_FOUND, exception.getErrorType(),
				"Exception type should be NOT_FOUND.");
		assertTrue(exception.getMessage().contains("Album with ID " + nonExistentAlbumId + " not found."),
				"Exception message should indicate album not found.");
	}

	// --- Helper method for direct DB verification ---
	private Song findSongByIdDirectly(int songId) throws SQLException {
		String query = "SELECT idSong, title, idAlbum, year, genre, audioFile, BIN_TO_UUID(idUser) as idUser FROM Song WHERE idSong = ?";
		try (PreparedStatement pStatement = connection.prepareStatement(query)) {
			pStatement.setInt(1, songId);
			try (ResultSet result = pStatement.executeQuery()) {
				if (result.next()) {
					Song song = new Song();
					song.setIdSong(result.getInt("idSong"));
					song.setTitle(result.getString("title"));
					song.setIdAlbum(result.getInt("idAlbum"));
					song.setYear(result.getInt("year"));
					song.setGenre(result.getString("genre"));
					song.setAudioFile(result.getString("audioFile"));
					song.setIdUser(UUID.fromString(result.getString("idUser")));
					return song;
				} else {
					return null; // Not found
				}
			}
		}
	}
}
