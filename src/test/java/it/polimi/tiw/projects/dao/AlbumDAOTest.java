package it.polimi.tiw.projects.dao;

import it.polimi.tiw.projects.beans.Album;
import it.polimi.tiw.projects.beans.User;
import it.polimi.tiw.projects.exceptions.DAOException;

import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AlbumDAOTest {

	private static final Logger logger = LoggerFactory.getLogger(AlbumDAOTest.class);

	private static Connection connection;
	private static AlbumDAO albumDAO;
	private static UserDAO userDAO;

	// Database connection details
	private static final String DB_URL = "jdbc:mysql://localhost:3306/TIW2025";
	private static final String DB_USER = "tiw";
	private static final String DB_PASS = "TIW2025";

	// Test Data - Use unique values to avoid conflicts during parallel testing or
	// leftover data
	private static final String TEST_ALBUM_NAME_1 = "JUnit Album 1 - " + System.currentTimeMillis();
	private static final String TEST_ALBUM_NAME_2 = "JUnit Album 2 - " + System.currentTimeMillis();
	private static final int TEST_ALBUM_YEAR_1 = 2024;
	private static final int TEST_ALBUM_YEAR_2 = 2025;
	private static final String TEST_ALBUM_ARTIST_1 = "JUnit Artist 1";
	private static final String TEST_ALBUM_ARTIST_2 = "JUnit Artist 2";
	private static final String TEST_ALBUM_IMAGE_1 = "path/to/image1.jpg";
	private static final String TEST_ALBUM_IMAGE_UPDATED = "path/to/new_image.png";
	private static final String TEST_ALBUM_NAME_UPDATED =
			"JUnit Album Updated - " + System.currentTimeMillis();
	private static final int TEST_ALBUM_YEAR_UPDATED = 2026;
	private static final String TEST_ALBUM_ARTIST_UPDATED = "JUnit Artist Updated";

	// Test User details
	private static final String TEST_USERNAME = "album_test_user_junit";
	private static final String TEST_PASSWORD = "password_album";
	private static final String TEST_NAME = "AlbumJUnit";
	private static final String TEST_SURNAME = "Tester";
	private static UUID testUserId;

	// Store IDs of created albums for cleanup/verification
	private Integer createdAlbumId1 = null;
	private Integer createdAlbumId2 = null;

	@BeforeAll
	void setUpClass() throws SQLException {
		try {
			// Ensure MySQL driver is loaded
			Class.forName("com.mysql.cj.jdbc.Driver");
			connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
			connection.setAutoCommit(false); // Manage transactions manually
			albumDAO = new AlbumDAO(connection);
			userDAO = new UserDAO(connection);
			logger.info("Database connection established for AlbumDAOTest.");

			// Initial cleanup of potential leftover test data (order matters: albums first)
			cleanupTestAlbums();
			cleanupTestUser();
			connection.commit();

			// Create the test user required for albums
			userDAO.createUser(TEST_USERNAME, TEST_PASSWORD, TEST_NAME, TEST_SURNAME);
			connection.commit(); // Commit user creation
			User testUser = userDAO.checkCredentials(TEST_USERNAME, TEST_PASSWORD);
			assertNotNull(testUser, "Test user could not be created or found.");
			testUserId = testUser.getIdUser();
			logger.info("Test user created with ID: {}", testUserId);

		} catch (ClassNotFoundException e) {
			logger.error("MySQL JDBC Driver not found", e);
			throw new SQLException("MySQL JDBC Driver not found.", e);
		} catch (SQLException e) {
			logger.error("Failed to connect to the database '{}'", DB_URL, e);
			throw e;
		} catch (DAOException e) { // Catch DAOException from user creation
			logger.error("DAOException during test user setup", e);
			if (connection != null)
				connection.rollback(); // Rollback if user creation failed
			throw new SQLException("Failed to setup test user", e); // Rethrow as SQLException for
																	// @BeforeAll
		}
	}

	@AfterAll
	void tearDownClass() throws SQLException {
		if (connection != null && !connection.isClosed()) {
			try {
				// Final cleanup (order matters: albums first)
				cleanupTestAlbums();
				cleanupTestUser();
				connection.commit(); // Commit final cleanup
			} catch (SQLException e) {
				logger.error("Error during final cleanup", e);
				connection.rollback(); // Attempt rollback on cleanup error
			} finally {
				connection.close();
				logger.info("Database connection closed for AlbumDAOTest.");
			}
		}
	}

	@BeforeEach
	void setUp() throws SQLException {
		// Clean albums before each test, commit the cleanup
		cleanupTestAlbums();
		connection.commit();
		createdAlbumId1 = null; // Reset stored IDs
		createdAlbumId2 = null;
	}

	@AfterEach
	void tearDown() throws SQLException {
		// Rollback any uncommitted changes from the test itself
		connection.rollback();
		// Clean up albums created during the test, commit the cleanup
		cleanupTestAlbums();
		connection.commit();
	}

	// Helper method to delete test albums based on stored IDs or name pattern
	private void cleanupTestAlbums() throws SQLException {
		// Delete by specific ID if known
		if (createdAlbumId1 != null) {
			deleteAlbumByIdDirectly(createdAlbumId1);
			createdAlbumId1 = null;
		}
		if (createdAlbumId2 != null) {
			deleteAlbumByIdDirectly(createdAlbumId2);
			createdAlbumId2 = null;
		}
		// Robust cleanup: Delete any albums matching the test name pattern AND user ID
		String deleteSQL = "DELETE FROM Album WHERE name LIKE ? AND idUser = UUID_TO_BIN(?)";
		try (PreparedStatement pStatement = connection.prepareStatement(deleteSQL)) {
			pStatement.setString(1, "JUnit Album %"); // Pattern matching
			if (testUserId != null) { // Only add user condition if testUserId was set
				pStatement.setString(2, testUserId.toString());
			} else {
				// If testUserId is null (e.g., setup failed), try deleting without user
				// condition
				// This might delete more than intended, but helps cleanup in error states.
				// Alternatively, use a different placeholder or handle error differently.
				// For simplicity, we'll assume testUserId is usually available for cleanup.
				// A safer approach might be to skip user-specific cleanup if testUserId is
				// null.
				logger.warn(
						"testUserId is null during cleanupTestAlbums. Cleanup might be incomplete.");
				// Re-prepare statement without user condition if necessary, or just return
				return; // Skip cleanup if user ID is missing
			}
			pStatement.executeUpdate();
		}
	}

	// Helper method to delete the test user
	private void cleanupTestUser() throws SQLException {
		if (testUserId != null) { // Only cleanup if user was potentially created
			String deleteSQL = "DELETE FROM User WHERE idUser = UUID_TO_BIN(?)";
			try (PreparedStatement pStatement = connection.prepareStatement(deleteSQL)) {
				pStatement.setString(1, testUserId.toString());
				pStatement.executeUpdate();
			}
			// Also try by username for robustness in case ID wasn't retrieved
			deleteSQL = "DELETE FROM User WHERE username = ?";
			try (PreparedStatement pStatement = connection.prepareStatement(deleteSQL)) {
				pStatement.setString(1, TEST_USERNAME);
				pStatement.executeUpdate();
			}
			testUserId = null; // Reset after cleanup attempt
		} else {
			// Robust cleanup by username if ID is null
			String deleteSQL = "DELETE FROM User WHERE username = ?";
			try (PreparedStatement pStatement = connection.prepareStatement(deleteSQL)) {
				pStatement.setString(1, TEST_USERNAME);
				pStatement.executeUpdate();
			}
		}
	}

	// Direct deletion bypassing DAO for cleanup robustness
	private void deleteAlbumByIdDirectly(int albumId) throws SQLException {
		String deleteSQL = "DELETE FROM Album WHERE idAlbum = ?";
		try (PreparedStatement pStatement = connection.prepareStatement(deleteSQL)) {
			pStatement.setInt(1, albumId);
			pStatement.executeUpdate();
		} catch (SQLException e) {
			if (!e.getSQLState().startsWith("23")) { // 23* are integrity constraint violations
				logger.warn("Error deleting album ID {} during cleanup: {}", albumId,
						e.getMessage(), e);
				// Optionally rethrow if it's not an FK issue: throw e;
			} else {
				logger.info(
						"Could not delete album ID {} due to likely FK constraint (songs exist?).",
						albumId);
			}
		}
	}

	// --- Test Cases ---

	@Test
	@Order(1)
	@DisplayName("Test successful album creation for a user (without image)")
	void testCreateAlbum_Success_NoImage() throws DAOException, SQLException {
		assertNotNull(testUserId, "Test User ID must be set before creating an album.");
		Album createdAlbum = assertDoesNotThrow(() -> albumDAO.createAlbum(TEST_ALBUM_NAME_1,
				TEST_ALBUM_YEAR_1, TEST_ALBUM_ARTIST_1, null, testUserId));

		assertNotNull(createdAlbum, "Created album object should not be null.");
		assertTrue(createdAlbum.getIdAlbum() > 0, "Created album ID should be positive.");
		createdAlbumId1 = createdAlbum.getIdAlbum(); // Store ID for cleanup/verification

		assertEquals(TEST_ALBUM_NAME_1, createdAlbum.getName());
		assertEquals(TEST_ALBUM_YEAR_1, createdAlbum.getYear());
		assertEquals(TEST_ALBUM_ARTIST_1, createdAlbum.getArtist());
		assertNull(createdAlbum.getImage(), "Image should be null when not provided.");
		assertEquals(testUserId, createdAlbum.getIdUser(), "Album user ID should match creator.");

		// Verify data in DB before commit (within the same transaction)
		Album foundAlbum = findAlbumByIdDirectly(createdAlbumId1);
		assertNotNull(foundAlbum, "Album should be findable in DB immediately after creation.");
		assertEquals(TEST_ALBUM_NAME_1, foundAlbum.getName());

		connection.commit(); // Commit the transaction

		// Verify data in DB after commit
		Album foundAlbumAfterCommit = findAlbumByIdDirectly(createdAlbumId1);
		assertNotNull(foundAlbumAfterCommit, "Album should be findable in DB after commit.");
		assertEquals(TEST_ALBUM_NAME_1, foundAlbumAfterCommit.getName());
	}

	@Test
	@Order(2)
	@DisplayName("Test creating album with duplicate name for the same user")
	void testCreateAlbum_DuplicateName_SameUser() throws DAOException, SQLException {
		assertNotNull(testUserId, "Test User ID must be set.");
		// First, create the album successfully (without image) and commit
		Album firstAlbum = albumDAO.createAlbum(TEST_ALBUM_NAME_1, TEST_ALBUM_YEAR_1,
				TEST_ALBUM_ARTIST_1, null, testUserId);
		createdAlbumId1 = firstAlbum.getIdAlbum();
		connection.commit();

		// Then, try to create another album with the same name FOR THE SAME USER
		// (without image)
		DAOException exception = assertThrows(DAOException.class, () -> {
			albumDAO.createAlbum(TEST_ALBUM_NAME_1, TEST_ALBUM_YEAR_2, TEST_ALBUM_ARTIST_2, null,
					testUserId);
			// This should fail, rollback will happen in @AfterEach
		}, "Creating an album with a duplicate name for the same user should throw DAOException.");

		// Check the updated error message from AlbumDAO
		assertEquals("Album name '" + TEST_ALBUM_NAME_1 + "' already exists for this user.",
				exception.getMessage());
		assertEquals(DAOException.DAOErrorType.NAME_ALREADY_EXISTS, exception.getErrorType());
	}

	@Test
	@Order(2) // Run alongside the other duplicate test
	@DisplayName("Test creating album with duplicate name for different users (should succeed)")
	void testCreateAlbum_DuplicateName_DifferentUsers() throws DAOException, SQLException {
		assertNotNull(testUserId, "Test User ID must be set.");
		// Create album for the primary test user (without image)
		Album firstAlbum = albumDAO.createAlbum(TEST_ALBUM_NAME_1, TEST_ALBUM_YEAR_1,
				TEST_ALBUM_ARTIST_1, null, testUserId);
		createdAlbumId1 = firstAlbum.getIdAlbum();
		connection.commit();

		// Create a second temporary user
		UserDAO tempUserDAO = new UserDAO(connection); // Use separate DAO instance if needed, or
														// reuse userDAO
		String tempUsername = "temp_album_user_" + System.currentTimeMillis();
		UUID tempUserId = null;
		try {
			tempUserDAO.createUser(tempUsername, "temp_pass", "Temp", "User");
			connection.commit();
			User tempUser = tempUserDAO.checkCredentials(tempUsername, "temp_pass");
			assertNotNull(tempUser, "Temporary user could not be created.");
			tempUserId = tempUser.getIdUser();

			// Create album with the SAME NAME but for the DIFFERENT USER (without image)
			UUID finalTempUserId = tempUserId; // Final variable for lambda
			Album secondAlbum = assertDoesNotThrow(
					() -> albumDAO.createAlbum(TEST_ALBUM_NAME_1, TEST_ALBUM_YEAR_2,
							TEST_ALBUM_ARTIST_2, null, finalTempUserId),
					"Creating album with same name for different user should succeed.");

			assertNotNull(secondAlbum);
			assertTrue(secondAlbum.getIdAlbum() > 0);
			assertNotEquals(createdAlbumId1, secondAlbum.getIdAlbum(),
					"Album IDs should be different.");
			assertEquals(TEST_ALBUM_NAME_1, secondAlbum.getName()); // Same name
			assertEquals(finalTempUserId, secondAlbum.getIdUser(),
					"Album should belong to the temporary user.");
			createdAlbumId2 = secondAlbum.getIdAlbum(); // Store for cleanup
			connection.commit();

		} finally {
			// Clean up the temporary user (regardless of test success/failure)
			if (tempUserId != null) {
				String deleteSQL = "DELETE FROM User WHERE idUser = UUID_TO_BIN(?)";
				try (PreparedStatement pStatement = connection.prepareStatement(deleteSQL)) {
					pStatement.setString(1, tempUserId.toString());
					pStatement.executeUpdate();
				}
			} else {
				// Robust cleanup by username if ID is null
				String deleteSQL = "DELETE FROM User WHERE username = ?";
				try (PreparedStatement pStatement = connection.prepareStatement(deleteSQL)) {
					pStatement.setString(1, tempUsername);
					pStatement.executeUpdate();
				}
			}
			connection.commit(); // Commit user cleanup
		}
	}

	@Test
	@Order(3)
	@DisplayName("Test finding an album by ID successfully (no image)")
	void testFindAlbumById_Success_NoImage() throws DAOException, SQLException {
		assertNotNull(testUserId, "Test User ID must be set.");
		// Create an album (without image) and commit
		Album originalAlbum = albumDAO.createAlbum(TEST_ALBUM_NAME_1, TEST_ALBUM_YEAR_1,
				TEST_ALBUM_ARTIST_1, null, testUserId);
		createdAlbumId1 = originalAlbum.getIdAlbum();
		connection.commit();

		// Find the album using the DAO
		Album foundAlbum = assertDoesNotThrow(() -> albumDAO.findAlbumById(createdAlbumId1));

		assertNotNull(foundAlbum, "Album should be found by its ID.");
		assertEquals(createdAlbumId1, foundAlbum.getIdAlbum());
		assertEquals(TEST_ALBUM_NAME_1, foundAlbum.getName());
		assertEquals(TEST_ALBUM_YEAR_1, foundAlbum.getYear());
		assertEquals(TEST_ALBUM_ARTIST_1, foundAlbum.getArtist());
		assertNull(foundAlbum.getImage(), "Found album image should be null.");
		assertEquals(testUserId, foundAlbum.getIdUser(),
				"Found album should belong to the correct user.");
	}

	@Test
	@Order(4)
	@DisplayName("Test finding an album by a non-existent ID")
	void testFindAlbumById_NotFound() {
		int nonExistentId = -999;
		// Expect DAOException with NOT_FOUND type
		DAOException exception = assertThrows(DAOException.class, () -> {
			albumDAO.findAlbumById(nonExistentId);
		}, "Finding a non-existent album ID should throw DAOException.");

		assertEquals(DAOException.DAOErrorType.NOT_FOUND, exception.getErrorType());
		assertTrue(
				exception.getMessage().contains("Album with ID " + nonExistentId + " not found."),
				"Exception message should indicate album not found.");
	}

	@Test
	@Order(5)
	@DisplayName("Test finding all albums")
	void testFindAllAlbums_Success() throws DAOException, SQLException {
		assertNotNull(testUserId, "Test User ID must be set.");
		// Create two albums for the test user (one with image, one without) and commit
		Album album1 = albumDAO.createAlbum(TEST_ALBUM_NAME_1, TEST_ALBUM_YEAR_1,
				TEST_ALBUM_ARTIST_1, TEST_ALBUM_IMAGE_1, testUserId);
		Album album2 = albumDAO.createAlbum(TEST_ALBUM_NAME_2, TEST_ALBUM_YEAR_2,
				TEST_ALBUM_ARTIST_2, null, testUserId);
		createdAlbumId1 = album1.getIdAlbum();
		createdAlbumId2 = album2.getIdAlbum();
		connection.commit();

		List<Album> allAlbums = assertDoesNotThrow(() -> albumDAO.findAllAlbums());

		assertNotNull(allAlbums);
		// Note: This assertion depends on the state of the DB. It should find AT LEAST
		// the two created albums.
		assertTrue(allAlbums.size() >= 2, "Should find at least the 2 created test albums.");

		// Verify our test albums are present and belong to the correct user
		assertTrue(
				allAlbums.stream().anyMatch(a -> a.getIdAlbum() == createdAlbumId1
						&& a.getName().equals(TEST_ALBUM_NAME_1) && testUserId.equals(a.getIdUser())
						&& TEST_ALBUM_IMAGE_1.equals(a.getImage())), // Check
																		// image
																		// too
				"Test album 1 not found or incorrect data in findAllAlbums result.");
		assertTrue(
				allAlbums.stream()
						.anyMatch(a -> a.getIdAlbum() == createdAlbumId2
								&& a.getName().equals(TEST_ALBUM_NAME_2)
								&& testUserId.equals(a.getIdUser()) && a.getImage() == null), // Check
																								// null
																								// image
				"Test album 2 not found or incorrect data in findAllAlbums result.");

		// Optional: Check sorting (if DAO guarantees it) - AlbumDAO sorts by artist,
		// year, name
		boolean sorted = true;
		for (int i = 0; i < allAlbums.size() - 1; i++) {
			Album current = allAlbums.get(i);
			Album next = allAlbums.get(i + 1);
			int artistCompare = current.getArtist().compareTo(next.getArtist());
			if (artistCompare > 0) {
				sorted = false;
				break;
			} else if (artistCompare == 0) {
				if (current.getYear() > next.getYear()) {
					sorted = false;
					break;
				} else if (current.getYear() == next.getYear()) {
					if (current.getName().compareTo(next.getName()) > 0) {
						sorted = false;
						break;
					}
				}
			}
		}
		assertTrue(sorted, "Albums list should be sorted by artist, year, name.");
	}

	@Test
	@Order(6)
	@DisplayName("Test finding all albums when DB is empty (after cleanup)")
	void testFindAllAlbums_Empty() throws DAOException, SQLException {
		// @BeforeEach ensures cleanup. Commit it.
		connection.commit();

		List<Album> allAlbums = assertDoesNotThrow(() -> albumDAO.findAllAlbums());
		assertNotNull(allAlbums);

		// Check that *our* test albums are not present
		assertFalse(allAlbums.stream().anyMatch(a -> a.getName().startsWith("JUnit Album")),
				"No JUnit test albums should be found after cleanup.");
	}

	@Test
	@Order(7)
	@DisplayName("Test successful full album update by owner (including image)")
	void testUpdateAlbum_Success_Full_WithImage() throws DAOException, SQLException {
		assertNotNull(testUserId, "Test User ID must be set.");
		// Create original album (without image) and commit
		Album originalAlbum = albumDAO.createAlbum(TEST_ALBUM_NAME_1, TEST_ALBUM_YEAR_1,
				TEST_ALBUM_ARTIST_1, null, testUserId);
		createdAlbumId1 = originalAlbum.getIdAlbum();
		connection.commit();

		// Update the album (as the owner) including the image
		assertDoesNotThrow(
				() -> albumDAO.updateAlbum(createdAlbumId1, testUserId, TEST_ALBUM_NAME_UPDATED,
						TEST_ALBUM_YEAR_UPDATED, TEST_ALBUM_ARTIST_UPDATED,
						TEST_ALBUM_IMAGE_UPDATED),
				"Successful update should not throw an exception.");
		connection.commit(); // Commit the update

		// Verify the update
		Album updatedAlbum = albumDAO.findAlbumById(createdAlbumId1);
		assertNotNull(updatedAlbum, "Updated album should still be findable.");
		assertEquals(TEST_ALBUM_NAME_UPDATED, updatedAlbum.getName());
		assertEquals(TEST_ALBUM_YEAR_UPDATED, updatedAlbum.getYear());
		assertEquals(TEST_ALBUM_ARTIST_UPDATED, updatedAlbum.getArtist());
		assertEquals(TEST_ALBUM_IMAGE_UPDATED, updatedAlbum.getImage(), "Image should be updated.");
	}

	@Test
	@Order(8)
	@DisplayName("Test partial album update (only name) by owner")
	void testUpdateAlbum_Success_Partial_Name() throws DAOException, SQLException {
		assertNotNull(testUserId, "Test User ID must be set.");
		Album originalAlbum = albumDAO.createAlbum(TEST_ALBUM_NAME_1, TEST_ALBUM_YEAR_1,
				TEST_ALBUM_ARTIST_1, TEST_ALBUM_IMAGE_1, testUserId);
		createdAlbumId1 = originalAlbum.getIdAlbum();
		connection.commit();

		// Update name only - pass null for other fields including image
		assertDoesNotThrow(
				() -> albumDAO.updateAlbum(createdAlbumId1, testUserId, TEST_ALBUM_NAME_UPDATED,
						null, null, null),
				"Successful partial name update should not throw an exception.");
		connection.commit();

		Album updatedAlbum = albumDAO.findAlbumById(createdAlbumId1);
		assertNotNull(updatedAlbum);
		assertEquals(TEST_ALBUM_NAME_UPDATED, updatedAlbum.getName(), "Name should be updated.");
		assertEquals(TEST_ALBUM_YEAR_1, updatedAlbum.getYear(), "Year should remain original.");
		assertEquals(TEST_ALBUM_ARTIST_1, updatedAlbum.getArtist(),
				"Artist should remain original.");
		assertEquals(TEST_ALBUM_IMAGE_1, updatedAlbum.getImage(), "Image should remain original.");
	}

	@Test
	@Order(9)
	@DisplayName("Test partial album update (only year) by owner")
	void testUpdateAlbum_Success_Partial_Year() throws DAOException, SQLException {
		assertNotNull(testUserId, "Test User ID must be set.");
		Album originalAlbum = albumDAO.createAlbum(TEST_ALBUM_NAME_1, TEST_ALBUM_YEAR_1,
				TEST_ALBUM_ARTIST_1, TEST_ALBUM_IMAGE_1, testUserId);
		createdAlbumId1 = originalAlbum.getIdAlbum();
		connection.commit();

		// Update year only - pass null for other fields including image
		assertDoesNotThrow(
				() -> albumDAO.updateAlbum(createdAlbumId1, testUserId, null,
						TEST_ALBUM_YEAR_UPDATED, null, null),
				"Successful partial year update should not throw an exception.");
		connection.commit();

		Album updatedAlbum = albumDAO.findAlbumById(createdAlbumId1);
		assertNotNull(updatedAlbum);
		assertEquals(TEST_ALBUM_NAME_1, updatedAlbum.getName(), "Name should remain original.");
		assertEquals(TEST_ALBUM_YEAR_UPDATED, updatedAlbum.getYear(), "Year should be updated.");
		assertEquals(TEST_ALBUM_ARTIST_1, updatedAlbum.getArtist(),
				"Artist should remain original.");
		assertEquals(TEST_ALBUM_IMAGE_1, updatedAlbum.getImage(), "Image should remain original.");
	}

	@Test
	@Order(10)
	@DisplayName("Test partial album update (only artist) by owner")
	void testUpdateAlbum_Success_Partial_Artist() throws DAOException, SQLException {
		assertNotNull(testUserId, "Test User ID must be set.");
		Album originalAlbum = albumDAO.createAlbum(TEST_ALBUM_NAME_1, TEST_ALBUM_YEAR_1,
				TEST_ALBUM_ARTIST_1, TEST_ALBUM_IMAGE_1, testUserId);
		createdAlbumId1 = originalAlbum.getIdAlbum();
		connection.commit();

		// Update artist only - pass null for other fields including image
		assertDoesNotThrow(
				() -> albumDAO.updateAlbum(createdAlbumId1, testUserId, null, null,
						TEST_ALBUM_ARTIST_UPDATED, null),
				"Successful partial artist update should not throw an exception.");
		connection.commit();

		Album updatedAlbum = albumDAO.findAlbumById(createdAlbumId1);
		assertNotNull(updatedAlbum);
		assertEquals(TEST_ALBUM_NAME_1, updatedAlbum.getName(), "Name should remain original.");
		assertEquals(TEST_ALBUM_YEAR_1, updatedAlbum.getYear(), "Year should remain original.");
		assertEquals(TEST_ALBUM_ARTIST_UPDATED, updatedAlbum.getArtist(),
				"Artist should be updated.");
		assertEquals(TEST_ALBUM_IMAGE_1, updatedAlbum.getImage(), "Image should remain original.");
	}

	@Test
	@Order(11)
	@DisplayName("Test updating a non-existent album")
	void testUpdateAlbum_NotFound() {
		assertNotNull(testUserId, "Test User ID must be set.");
		int nonExistentId = -999;
		DAOException exception = assertThrows(DAOException.class, () -> {
			// Attempt update with the test user ID, but non-existent album ID (include
			// image param)
			albumDAO.updateAlbum(nonExistentId, testUserId, TEST_ALBUM_NAME_UPDATED,
					TEST_ALBUM_YEAR_UPDATED, TEST_ALBUM_ARTIST_UPDATED, TEST_ALBUM_IMAGE_UPDATED);
			// Rollback will happen in @AfterEach
		});

		// Check the updated error message from AlbumDAO
		assertEquals(
				"Album with ID " + nonExistentId + " not found for update or user not authorized.",
				exception.getMessage());
		assertEquals(DAOException.DAOErrorType.NOT_FOUND, exception.getErrorType());
	}

	@Test
	@Order(12)
	@DisplayName("Test updating album to a duplicate name for the same user")
	void testUpdateAlbum_DuplicateName() throws DAOException, SQLException {
		assertNotNull(testUserId, "Test User ID must be set.");
		// Create two albums for the same user (without images) and commit
		Album album1 = albumDAO.createAlbum(TEST_ALBUM_NAME_1, TEST_ALBUM_YEAR_1,
				TEST_ALBUM_ARTIST_1, null, testUserId);
		Album album2 = albumDAO.createAlbum(TEST_ALBUM_NAME_2, TEST_ALBUM_YEAR_2,
				TEST_ALBUM_ARTIST_2, null, testUserId);
		createdAlbumId1 = album1.getIdAlbum();
		createdAlbumId2 = album2.getIdAlbum();
		connection.commit();

		// Try updating album2 to have the same name as album1 (for the same user)
		DAOException exception = assertThrows(DAOException.class, () -> {
			// Update name only, pass null for others including image
			albumDAO.updateAlbum(createdAlbumId2, testUserId, TEST_ALBUM_NAME_1, null, null, null);
			// Rollback will happen in @AfterEach
		});

		// Check the updated error message
		assertEquals("Album name '" + TEST_ALBUM_NAME_1 + "' already exists for this user.",
				exception.getMessage());
		assertEquals(DAOException.DAOErrorType.NAME_ALREADY_EXISTS, exception.getErrorType());
	}

	@Test
	@Order(13)
	@DisplayName("Test updating album with no fields provided")
	void testUpdateAlbum_NoFields() throws DAOException, SQLException {
		assertNotNull(testUserId, "Test User ID must be set.");
		// Create an album first (without image)
		Album album1 = albumDAO.createAlbum(TEST_ALBUM_NAME_1, TEST_ALBUM_YEAR_1,
				TEST_ALBUM_ARTIST_1, null, testUserId);
		createdAlbumId1 = album1.getIdAlbum();
		connection.commit();

		// Try updating with all nulls (should throw IllegalArgumentException before DAO
		// call)
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
			albumDAO.updateAlbum(createdAlbumId1, testUserId, null, null, null, null);
		});

		assertEquals("No fields provided for update.", exception.getMessage());
	}

	@Test
	@Order(14)
	@DisplayName("Test deleting an album successfully by owner")
	void testDeleteAlbum_Success() throws DAOException, SQLException {
		assertNotNull(testUserId, "Test User ID must be set.");
		// Create an album (without image) and commit
		Album albumToDelete = albumDAO.createAlbum(TEST_ALBUM_NAME_1, TEST_ALBUM_YEAR_1,
				TEST_ALBUM_ARTIST_1, null, testUserId);
		createdAlbumId1 = albumToDelete.getIdAlbum();
		connection.commit();

		// Verify it exists before delete
		assertNotNull(findAlbumByIdDirectly(createdAlbumId1),
				"Album should exist before deletion.");

		// Delete the album using DAO (as the owner) - Now returns void
		assertDoesNotThrow(() -> albumDAO.deleteAlbum(createdAlbumId1, testUserId),
				"Successful delete should not throw an exception.");
		connection.commit(); // Commit the deletion

		// Verify it's gone using DAO find method (which now throws NOT_FOUND)
		DAOException findException = assertThrows(DAOException.class, () -> {
			albumDAO.findAlbumById(createdAlbumId1);
		}, "Finding deleted album should throw DAOException.");
		assertEquals(DAOException.DAOErrorType.NOT_FOUND, findException.getErrorType());

		// Also verify directly
		assertNull(findAlbumByIdDirectly(createdAlbumId1),
				"Album should not be findable directly after deletion.");

		createdAlbumId1 = null; // Nullify ID as it's successfully deleted
	}

	@Test
	@Order(15)
	@DisplayName("Test deleting a non-existent album")
	void testDeleteAlbum_NotFound() {
		assertNotNull(testUserId, "Test User ID must be set.");
		int nonExistentId = -999;

		DAOException exception = assertThrows(DAOException.class, () -> {
			albumDAO.deleteAlbum(nonExistentId, testUserId);
			// Rollback will happen in @AfterEach
		});

		assertEquals(
				"Album with ID " + nonExistentId
						+ " not found for deletion or user not authorized.",
				exception.getMessage());
		assertEquals(DAOException.DAOErrorType.NOT_FOUND, exception.getErrorType());
	}

	@Test
	@Order(16)
	@DisplayName("Test finding albums by user successfully")
	void testFindAlbumsByUser_Success() throws DAOException, SQLException {
		assertNotNull(testUserId, "Test User ID must be set.");
		// Create two albums for the test user (one with image, one without)
		Album album1 = albumDAO.createAlbum(TEST_ALBUM_NAME_1, TEST_ALBUM_YEAR_1,
				TEST_ALBUM_ARTIST_1, TEST_ALBUM_IMAGE_1, testUserId);
		Album album2 = albumDAO.createAlbum(TEST_ALBUM_NAME_2, TEST_ALBUM_YEAR_2,
				TEST_ALBUM_ARTIST_2, null, testUserId);
		createdAlbumId1 = album1.getIdAlbum();
		createdAlbumId2 = album2.getIdAlbum();
		connection.commit();

		// Find albums for this user
		List<Album> userAlbums = assertDoesNotThrow(() -> albumDAO.findAlbumsByUser(testUserId));

		assertNotNull(userAlbums);
		assertEquals(2, userAlbums.size(), "Should find exactly 2 albums for the test user.");

		// Verify the found albums match the created ones (check IDs and user ID)
		assertTrue(userAlbums.stream().anyMatch(
				a -> a.getIdAlbum() == createdAlbumId1 && testUserId.equals(a.getIdUser())));
		assertTrue(userAlbums.stream().anyMatch(
				a -> a.getIdAlbum() == createdAlbumId2 && testUserId.equals(a.getIdUser())));
	}

	@Test
	@Order(17)
	@DisplayName("Test finding albums for a user with no albums")
	void testFindAlbumsByUser_NotFound() throws DAOException {
		// Use a random UUID for a user guaranteed to have no albums
		UUID nonExistentUserId = UUID.randomUUID();
		List<Album> userAlbums =
				assertDoesNotThrow(() -> albumDAO.findAlbumsByUser(nonExistentUserId));

		assertNotNull(userAlbums);
		assertTrue(userAlbums.isEmpty(),
				"Should find no albums for a user who hasn't created any.");
	}

	@Test
	@Order(18)
	@DisplayName("Test updating an album by non-owner (unauthorized)")
	void testUpdateAlbum_Unauthorized() throws DAOException, SQLException {
		assertNotNull(testUserId, "Test User ID must be set.");
		// Create an album with the test user (without image)
		Album originalAlbum = albumDAO.createAlbum(TEST_ALBUM_NAME_1, TEST_ALBUM_YEAR_1,
				TEST_ALBUM_ARTIST_1, null, testUserId);
		createdAlbumId1 = originalAlbum.getIdAlbum();
		connection.commit();

		// Generate a different, random user ID
		UUID unauthorizedUserId = UUID.randomUUID();
		assertNotEquals(testUserId, unauthorizedUserId);

		// Attempt to update the album using the unauthorized user ID (include image
		// param)
		DAOException exception = assertThrows(DAOException.class, () -> {
			albumDAO.updateAlbum(createdAlbumId1, unauthorizedUserId, // Wrong user ID
					TEST_ALBUM_NAME_UPDATED, null, null, TEST_ALBUM_IMAGE_UPDATED);
		});

		// Check the error message and type (expecting NOT_FOUND or a specific
		// UNAUTHORIZED if implemented)
		assertEquals("Album with ID " + createdAlbumId1
				+ " not found for update or user not authorized.", exception.getMessage());
		assertEquals(DAOException.DAOErrorType.NOT_FOUND, exception.getErrorType()); // Assuming
																						// NOT_FOUND
																						// is used
																						// for
																						// simplicity

		// Verify the album was NOT actually updated
		Album albumAfterAttempt = findAlbumByIdDirectly(createdAlbumId1);
		assertNotNull(albumAfterAttempt);
		assertEquals(TEST_ALBUM_NAME_1, albumAfterAttempt.getName(),
				"Album name should not have been updated.");
	}

	@Test
	@Order(19)
	@DisplayName("Test deleting an album by non-owner (unauthorized)")
	void testDeleteAlbum_Unauthorized() throws DAOException, SQLException {
		assertNotNull(testUserId, "Test User ID must be set.");
		// Create an album with the test user (without image)
		Album albumToDelete = albumDAO.createAlbum(TEST_ALBUM_NAME_1, TEST_ALBUM_YEAR_1,
				TEST_ALBUM_ARTIST_1, null, testUserId);
		createdAlbumId1 = albumToDelete.getIdAlbum();
		connection.commit();

		// Generate a different, random user ID
		UUID unauthorizedUserId = UUID.randomUUID();
		assertNotEquals(testUserId, unauthorizedUserId);

		// Attempt to delete the album using the unauthorized user ID
		DAOException exception = assertThrows(DAOException.class, () -> {
			albumDAO.deleteAlbum(createdAlbumId1, unauthorizedUserId); // Wrong user ID
		});

		// Check the error message and type
		assertEquals(
				"Album with ID " + createdAlbumId1
						+ " not found for deletion or user not authorized.",
				exception.getMessage());
		assertEquals(DAOException.DAOErrorType.NOT_FOUND, exception.getErrorType()); // Assuming
																						// NOT_FOUND

		// Verify the album was NOT actually deleted
		assertNotNull(findAlbumByIdDirectly(createdAlbumId1),
				"Album should still exist after unauthorized delete attempt.");
	}

	// --- Helper method for direct DB verification (includes idUser and image) ---
	private Album findAlbumByIdDirectly(int albumId) throws SQLException {
		// Added image to SELECT
		String query =
				"SELECT idAlbum, name, year, artist, image, BIN_TO_UUID(idUser) as idUser FROM Album WHERE idAlbum = ?";
		try (PreparedStatement pStatement = connection.prepareStatement(query)) {
			pStatement.setInt(1, albumId);
			try (ResultSet result = pStatement.executeQuery()) {
				if (result.next()) {
					Album album = new Album();
					album.setIdAlbum(result.getInt("idAlbum"));
					album.setName(result.getString("name"));
					album.setYear(result.getInt("year"));
					album.setArtist(result.getString("artist"));
					album.setImage(result.getString("image"));
					String userIdStr = result.getString("idUser");
					if (userIdStr != null) {
						album.setIdUser(UUID.fromString(userIdStr));
					}
					return album;
				} else {
					return null; // Not found
				}
			}
		}
	}
}
