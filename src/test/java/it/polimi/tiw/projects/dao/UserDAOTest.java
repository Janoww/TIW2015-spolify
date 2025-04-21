package it.polimi.tiw.projects.dao;

import static org.junit.jupiter.api.Assertions.*;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.UUID;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;

import it.polimi.tiw.projects.beans.User;
import it.polimi.tiw.projects.exceptions.DAOException;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class UserDAOTest {

	private static Connection connection;
	private static UserDAO userDAO;
	private static final String DB_URL = "jdbc:mysql://localhost:3306/TIW2025";
	private static final String DB_USER = "tiw";
	private static final String DB_PASS = "TIW2025";

	// Test user details - use unique names to avoid conflicts
	private static final String TEST_USERNAME = "testuser_junit";
	private static final String TEST_PASSWORD = "password123";
	private static final String TEST_NAME = "JUnit";
	private static final String TEST_SURNAME = "Tester";
	private static final String TEST_NAME_MODIFIED = "JUnitModified";
	private static final String TEST_SURNAME_MODIFIED = "TesterModified";

	@BeforeAll
	public void setUpClass() throws SQLException {
		try {
			// Ensure MySQL driver is loaded (often automatic, but good practice)
			Class.forName("com.mysql.cj.jdbc.Driver");

			connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
			// Disable auto-commit to manage transactions manually for tests
			connection.setAutoCommit(false);
			userDAO = new UserDAO(connection);
			System.out.println("Database connection established for tests (DB: " + DB_URL + ").");

		} catch (ClassNotFoundException e) { // Added catch block for ClassNotFoundException
			System.err.println("MySQL JDBC Driver not found: " + e.getMessage());
			throw new SQLException("MySQL JDBC Driver not found.", e);
		} catch (SQLException e) {
			System.err.println("Failed to connect to the database '" + DB_URL + "': " + e.getMessage());
			e.printStackTrace();
			// If connection fails, tests cannot run.
			throw e;
		}
		// Initial cleanup in case previous tests failed mid-execution
		cleanupTestUser();
		connection.commit(); // Commit the initial cleanup
	}

	@AfterAll
	public void tearDownClass() throws SQLException {
		// Final cleanup
		cleanupTestUser();
		if (connection != null && !connection.isClosed()) {
			connection.commit(); // Commit final cleanup
			connection.close();
			System.out.println("Database connection closed.");
		}
	}

	@BeforeEach
	public void setUp() throws SQLException {
		// Ensure clean state before each test by removing the test user
		// This might be redundant if cleanupTestUser works correctly in
		// AfterEach/AfterAll,
		// but provides extra safety.
		cleanupTestUser();
		connection.commit(); // Commit cleanup before starting test
	}

	@AfterEach
	public void tearDown() throws SQLException {
		// Rollback any uncommitted changes from the test itself
		// This helps isolate tests, ensuring a failed test doesn't leave partial data
		connection.rollback();
		// Clean up the specific test user created during the test
		cleanupTestUser();
		connection.commit(); // Commit the cleanup after rollback
	}

	// Helper method to delete the test user using its unique username
	private void cleanupTestUser() throws SQLException {
		// Use PreparedStatement for safety, even in cleanup
		String deleteSQL = "DELETE FROM User WHERE username = ?"; // Corrected table name case
		try (PreparedStatement pStatement = connection.prepareStatement(deleteSQL)) {
			pStatement.setString(1, TEST_USERNAME);
			pStatement.executeUpdate();
			// System.out.println("Cleanup executed for user: " + TEST_USERNAME);
		}
	}

	// --- Test Cases ---

	@Test
	@Order(1)
	public void testCreateUser_Success() {
		assertDoesNotThrow(() -> {
			userDAO.createUser(TEST_USERNAME, TEST_PASSWORD, TEST_NAME, TEST_SURNAME);
			// No commit here yet, let checkCredentials verify before committing
		}, "User creation method call should not throw an exception.");

		// Verify creation by trying to log in (within the same transaction)
		assertDoesNotThrow(() -> {
			User user = userDAO.checkCredentials(TEST_USERNAME, TEST_PASSWORD);
			assertNotNull(user, "User should be found immediately after creation (before commit).");
			assertEquals(TEST_USERNAME, user.getUsername());
			assertEquals(TEST_NAME, user.getName());
			assertEquals(TEST_SURNAME, user.getSurname());
		}, "Checking credentials immediately after creation should succeed.");

		// Now commit the transaction
		assertDoesNotThrow(() -> connection.commit(), "Commit after successful creation should succeed.");
	}

	@Test
	@Order(2)
	public void testCreateUser_Duplicate() throws DAOException, SQLException {
		// First, create the user successfully and commit
		userDAO.createUser(TEST_USERNAME, TEST_PASSWORD, TEST_NAME, TEST_SURNAME);
		connection.commit();

		// Then, try to create the same user again
		DAOException exception = assertThrows(DAOException.class, () -> {
			userDAO.createUser(TEST_USERNAME, TEST_PASSWORD, "Another", "User");
			// This should fail, so no commit needed/expected here.
			// If it didn't throw, the test would fail anyway.
		}, "Creating a duplicate user should throw DAOException.");

		assertEquals("Username already exists", exception.getMessage());
		assertEquals(DAOException.DAOErrorType.NAME_ALREADY_EXISTS, exception.getErrorType());
		// No explicit rollback needed here, AfterEach handles it.
	}

	@Test
	@Order(3)
	public void testCheckCredentials_Success() throws DAOException, SQLException {
		// Create user first and commit
		userDAO.createUser(TEST_USERNAME, TEST_PASSWORD, TEST_NAME, TEST_SURNAME);
		connection.commit();

		// Check credentials in a new logical operation (though same connection)
		User user = assertDoesNotThrow(() -> userDAO.checkCredentials(TEST_USERNAME, TEST_PASSWORD),
				"Valid credentials check should not throw exception.");

		assertNotNull(user, "User object should not be null for valid credentials.");
		assertEquals(TEST_USERNAME, user.getUsername());
		assertEquals(TEST_NAME, user.getName());
		assertEquals(TEST_SURNAME, user.getSurname());
		assertNotNull(user.getIdUser(), "User ID (UUID) should not be null."); // Check for non-null UUID
	}

	@Test
	@Order(4)
	public void testCheckCredentials_InvalidUsername() {
		// Ensure no user with TEST_USERNAME exists (handled by BeforeEach/AfterEach)
		DAOException exception = assertThrows(DAOException.class, () -> {
			userDAO.checkCredentials("nonexistentuser" + System.currentTimeMillis(), TEST_PASSWORD);
		}, "Checking credentials for non-existent user should throw DAOException.");

		assertEquals("Invalid credentials", exception.getMessage());
		assertEquals(DAOException.DAOErrorType.INVALID_CREDENTIALS, exception.getErrorType());
	}

	@Test
	@Order(5)
	public void testCheckCredentials_InvalidPassword() throws DAOException, SQLException {
		// Create user first and commit
		userDAO.createUser(TEST_USERNAME, TEST_PASSWORD, TEST_NAME, TEST_SURNAME);
		connection.commit();

		// Check with wrong password
		DAOException exception = assertThrows(DAOException.class, () -> {
			userDAO.checkCredentials(TEST_USERNAME, "wrongpassword" + System.currentTimeMillis());
		}, "Checking credentials with wrong password should throw DAOException.");

		assertEquals("Invalid credentials", exception.getMessage());
		assertEquals(DAOException.DAOErrorType.INVALID_CREDENTIALS, exception.getErrorType());
	}

	@Test
	@Order(6)
	public void testModifyUser_Success() throws DAOException, SQLException {
		userDAO.createUser(TEST_USERNAME, TEST_PASSWORD, TEST_NAME, TEST_SURNAME);
		connection.commit();

		User userToModify = userDAO.checkCredentials(TEST_USERNAME, TEST_PASSWORD);
		assertNotNull(userToModify, "Failed to retrieve user before modification.");
		UUID originalId = userToModify.getIdUser(); // Store UUID for verification

		assertDoesNotThrow(() -> {
			userDAO.modifyUser(userToModify, TEST_NAME_MODIFIED, TEST_SURNAME_MODIFIED);
			// Don't commit yet, verify within the same transaction first
		}, "Modification method call should not throw an exception.");

		User modifiedUserCheck1 = userDAO.checkCredentials(TEST_USERNAME, TEST_PASSWORD);
		assertNotNull(modifiedUserCheck1, "Failed to retrieve user immediately after modification.");
		assertEquals(TEST_NAME_MODIFIED, modifiedUserCheck1.getName(), "Name should be updated immediately.");
		assertEquals(TEST_SURNAME_MODIFIED, modifiedUserCheck1.getSurname(), "Surname should be updated immediately.");
		assertEquals(originalId, modifiedUserCheck1.getIdUser(), "User ID should remain the same.");

		assertDoesNotThrow(() -> connection.commit(), "Commit after successful modification should succeed.");

		User modifiedUserCheck2 = userDAO.checkCredentials(TEST_USERNAME, TEST_PASSWORD);
		assertNotNull(modifiedUserCheck2, "Failed to retrieve user after commit.");
		assertEquals(TEST_NAME_MODIFIED, modifiedUserCheck2.getName(), "Name should be updated after commit.");
		assertEquals(TEST_SURNAME_MODIFIED, modifiedUserCheck2.getSurname(), "Surname should be updated after commit.");
		assertEquals(originalId, modifiedUserCheck2.getIdUser(), "User ID should remain the same after commit.");
	}

	@Test
	@Order(7)
	public void testModifyUser_NullValues() throws DAOException, SQLException {
		userDAO.createUser(TEST_USERNAME, TEST_PASSWORD, TEST_NAME, TEST_SURNAME);
		connection.commit();

		User userToModify = userDAO.checkCredentials(TEST_USERNAME, TEST_PASSWORD);
		assertNotNull(userToModify);

		assertDoesNotThrow(() -> {
			userDAO.modifyUser(userToModify, null, null);
			connection.commit(); // Commit the (non-)modification
		}, "Modification with nulls should not throw an exception.");

		User notModifiedUser = userDAO.checkCredentials(TEST_USERNAME, TEST_PASSWORD);
		assertNotNull(notModifiedUser);
		assertEquals(TEST_NAME, notModifiedUser.getName(), "Name should remain the original.");
		assertEquals(TEST_SURNAME, notModifiedUser.getSurname(), "Surname should remain the original.");
	}

	@Test
	@Order(8)
	public void testModifyUser_OnlyName() throws DAOException, SQLException {

		userDAO.createUser(TEST_USERNAME, TEST_PASSWORD, TEST_NAME, TEST_SURNAME);
		connection.commit();

		User userToModify = userDAO.checkCredentials(TEST_USERNAME, TEST_PASSWORD);
		assertNotNull(userToModify);

		assertDoesNotThrow(() -> {
			userDAO.modifyUser(userToModify, TEST_NAME_MODIFIED, null);
			connection.commit(); // Commit the modification
		}, "Modification of only name should not throw an exception.");

		User modifiedUser = userDAO.checkCredentials(TEST_USERNAME, TEST_PASSWORD);
		assertNotNull(modifiedUser);
		assertEquals(TEST_NAME_MODIFIED, modifiedUser.getName(), "Name should be updated.");
		assertEquals(TEST_SURNAME, modifiedUser.getSurname(), "Surname should remain the original.");
	}

	@Test
	@Order(9)
	public void testModifyUser_OnlySurname() throws DAOException, SQLException {
		userDAO.createUser(TEST_USERNAME, TEST_PASSWORD, TEST_NAME, TEST_SURNAME);
		connection.commit();

		User userToModify = userDAO.checkCredentials(TEST_USERNAME, TEST_PASSWORD);
		assertNotNull(userToModify);

		assertDoesNotThrow(() -> {
			userDAO.modifyUser(userToModify, null, TEST_SURNAME_MODIFIED);
			connection.commit(); // Commit the modification
		}, "Modification of only surname should not throw an exception.");

		User modifiedUser = userDAO.checkCredentials(TEST_USERNAME, TEST_PASSWORD);
		assertNotNull(modifiedUser);
		assertEquals(TEST_NAME, modifiedUser.getName(), "Name should remain the original.");
		assertEquals(TEST_SURNAME_MODIFIED, modifiedUser.getSurname(), "Surname should be updated.");
	}
}
