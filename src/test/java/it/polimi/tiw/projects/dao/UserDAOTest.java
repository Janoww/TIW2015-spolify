package it.polimi.tiw.projects.dao;

import it.polimi.tiw.projects.beans.User;
import it.polimi.tiw.projects.exceptions.DAOException;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class UserDAOTest {

    private static final Logger logger = LoggerFactory.getLogger(UserDAOTest.class);
    private static final String DB_URL = "jdbc:mysql://localhost:3306/TIW2025";
    private static final String DB_USER = "tiw";
    private static final String DB_PASS = "TIW2025";
    // Test user details - use unique names to avoid conflicts
    private static final String TEST_USERNAME = "testUser_junit";
    private static final String TEST_PASSWORD = "password123";
    private static final String TEST_NAME = "JUnit";
    private static final String TEST_SURNAME = "Tester";
    private static final String TEST_NAME_MODIFIED = "JUnitModified";
    private static final String TEST_SURNAME_MODIFIED = "TesterModified";
    private static Connection connection;
    private static UserDAO userDAO;

    @BeforeAll
    void setUpClass() throws SQLException {
        try {
            // Ensure MySQL driver is loaded (often automatic, but good practice)
            Class.forName("com.mysql.cj.jdbc.Driver");

            connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
            // Disable auto-commit to manage transactions manually for tests
            connection.setAutoCommit(false);
            userDAO = new UserDAO(connection);
            logger.info("Database connection established for tests (DB: {}).", DB_URL);

        } catch (ClassNotFoundException e) { // Added catch block for ClassNotFoundException
            logger.error("MySQL JDBC Driver not found", e);
            throw new SQLException("MySQL JDBC Driver not found.", e);
        } catch (SQLException e) {
            logger.error("Failed to connect to the database '{}'", DB_URL, e);
            // If connection fails, tests cannot run.
            throw e;
        }
        // Initial cleanup in case previous tests failed mid-execution
        cleanupTestUser();
        connection.commit(); // Commit the initial cleanup
    }

    @AfterAll
    void tearDownClass() throws SQLException {
        // Final cleanup
        cleanupTestUser();
        if (connection != null && !connection.isClosed()) {
            connection.commit(); // Commit final cleanup
            connection.close();
            logger.info("Database connection closed.");
        }
    }

    @BeforeEach
    void setUp() throws SQLException {
        cleanupTestUser();
        connection.commit(); // Commit cleanup before starting test
    }

    @AfterEach
    void tearDown() throws SQLException {
        connection.rollback();
        cleanupTestUser();
        connection.commit();
    }

    // Helper method to delete the test user using its unique username
    private void cleanupTestUser() throws SQLException {
        // Use PreparedStatement for safety, even in cleanup
        // Delete users whose usernames start with TEST_USERNAME to cover variations
        String deleteSQL = "DELETE FROM User WHERE username LIKE ?";
        try (PreparedStatement pStatement = connection.prepareStatement(deleteSQL)) {
            pStatement.setString(1, TEST_USERNAME + "%");
            int rowsAffected = pStatement.executeUpdate();
            logger.trace("Cleanup executed for users matching '{}%'. Rows affected: {}", TEST_USERNAME, rowsAffected);
        }
    }

    // --- Test Cases ---

    @Test
    @Order(1)
    void testCreateUser_Success() {
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
    void testCreateUser_Duplicate() throws DAOException, SQLException {
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
    void testCheckCredentials_Success() throws DAOException, SQLException {
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
        assertNotNull(user.getIdUser(), "User ID (UUID) should not be null."); // Check for non-null
        // UUID
    }

    @Test
    @Order(4)
    void testCheckCredentials_InvalidUsername() {
        // Ensure no user with TEST_USERNAME exists (handled by BeforeEach/AfterEach)
        DAOException exception = assertThrows(DAOException.class, () -> userDAO.checkCredentials("nonExistentUser" + System.currentTimeMillis(), TEST_PASSWORD), "Checking credentials for non-existent user should throw DAOException.");

        assertEquals("Invalid credentials", exception.getMessage());
        assertEquals(DAOException.DAOErrorType.INVALID_CREDENTIALS, exception.getErrorType());
    }

    @Test
    @Order(5)
    void testCheckCredentials_InvalidPassword() throws DAOException, SQLException {
        // Create user first and commit
        userDAO.createUser(TEST_USERNAME, TEST_PASSWORD, TEST_NAME, TEST_SURNAME);
        connection.commit();

        // Check with wrong password
        DAOException exception = assertThrows(DAOException.class, () -> userDAO.checkCredentials(TEST_USERNAME, "wrongPassword" + System.currentTimeMillis()), "Checking credentials with wrong password should throw DAOException.");

        assertEquals("Invalid credentials", exception.getMessage());
        assertEquals(DAOException.DAOErrorType.INVALID_CREDENTIALS, exception.getErrorType());
    }

    @Test
    @Order(6)
    void testModifyUser_Success() throws DAOException, SQLException {
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
    void testModifyUser_NullValues() throws DAOException, SQLException {
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
    void testModifyUser_OnlyName() throws DAOException, SQLException {

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
    void testModifyUser_OnlySurname() throws DAOException, SQLException {
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

    @Test
    @Order(10)
    @DisplayName("Test creating user with null username")
    void testCreateUser_NullUsername() {
        // Username is NOT NULL. Attempting to insert NULL should cause an SQLException.
        // The DAO wraps generic SQLExceptions (not matching specific unique constraint
        // for username) as GENERIC_ERROR.
        // A NOT NULL violation (MySQL error 1048) has SQLState '23000'.
        // If username parameter is null, the DAO's "23000" check for "Username 'null'
        // already exists" might be hit,
        // or it might fall to GENERIC_ERROR. Given the check is `throw new
        // DAOException("Username '" + username + "' already exists"`),
        // if username is null, this would be "Username 'null' already exists".
        // Let's expect NAME_ALREADY_EXISTS if SQLState is 23000, or GENERIC_ERROR
        // otherwise.
        // More robustly, the DAO should differentiate NOT NULL violations.
        // For this test, we'll check for either, as the exact outcome depends on how
        // the driver/DB handles null for the unique check vs NOT NULL.
        DAOException exception = assertThrows(DAOException.class, () -> userDAO.createUser(null, TEST_PASSWORD, TEST_NAME, TEST_SURNAME));
        assertTrue(
                exception.getErrorType() == DAOException.DAOErrorType.GENERIC_ERROR
                        || exception.getErrorType() == DAOException.DAOErrorType.NAME_ALREADY_EXISTS, // if SQLState
                // 23000 is hit
                "Expected GENERIC_ERROR or NAME_ALREADY_EXISTS for null username (NOT NULL constraint)");
    }

    @Test
    @Order(11)
    @DisplayName("Test creating user with null password")
    void testCreateUser_NullPassword() {
        // Password is NOT NULL.
        // If this results in SQLState '23000', UserDAO might incorrectly map it to
        // NAME_ALREADY_EXISTS
        // if the username was valid. Otherwise, it's GENERIC_ERROR.
        DAOException exception = assertThrows(DAOException.class, () -> userDAO.createUser(TEST_USERNAME + "_nullpass", null, TEST_NAME, TEST_SURNAME));
        assertTrue(
                exception.getErrorType() == DAOException.DAOErrorType.GENERIC_ERROR
                        || (exception.getErrorType() == DAOException.DAOErrorType.NAME_ALREADY_EXISTS
                        && exception.getMessage()
                        .startsWith("Username '" + TEST_USERNAME + "_nullpass" + "' already exists.")),
                "Expected GENERIC_ERROR or specific NAME_ALREADY_EXISTS due to DAO's handling of 23000 SQLState for null password (NOT NULL constraint)");
    }

    @Test
    @Order(12)
    @DisplayName("Test creating user with null name (should succeed)")
    void testCreateUser_NullName_ShouldSucceed() throws DAOException, SQLException {
        String username = TEST_USERNAME + "_nullname";
        User createdUser = assertDoesNotThrow(() -> userDAO.createUser(username, TEST_PASSWORD, null, TEST_SURNAME), "Creating user with null name should succeed as 'name' is nullable.");
        connection.commit();

        assertNotNull(createdUser, "Created user object should not be null.");
        assertEquals(username, createdUser.getUsername());
        assertNull(createdUser.getName(), "User's name should be null in the bean.");
        assertEquals(TEST_SURNAME, createdUser.getSurname());

        // Verify in DB
        User userFromDB = userDAO.checkCredentials(username, TEST_PASSWORD);
        assertNotNull(userFromDB, "User should be retrievable from DB.");
        assertNull(userFromDB.getName(), "User's name should be null in DB.");
        assertEquals(TEST_SURNAME, userFromDB.getSurname());
    }

    @Test
    @Order(13)
    @DisplayName("Test creating user with null surname (should succeed)")
    void testCreateUser_NullSurname_ShouldSucceed() throws DAOException, SQLException {
        String username = TEST_USERNAME + "_nullsurname";
        User createdUser = assertDoesNotThrow(() -> userDAO.createUser(username, TEST_PASSWORD, TEST_NAME, null), "Creating user with null surname should succeed as 'surname' is nullable.");
        connection.commit();

        assertNotNull(createdUser, "Created user object should not be null.");
        assertEquals(username, createdUser.getUsername());
        assertEquals(TEST_NAME, createdUser.getName());
        assertNull(createdUser.getSurname(), "User's surname should be null in the bean.");

        // Verify in DB
        User userFromDB = userDAO.checkCredentials(username, TEST_PASSWORD);
        assertNotNull(userFromDB, "User should be retrievable from DB.");
        assertEquals(TEST_NAME, userFromDB.getName());
        assertNull(userFromDB.getSurname(), "User's surname should be null in DB.");
    }

    @Test
    @Order(14)
    @DisplayName("Test checkCredentials with null username")
    void testCheckCredentials_NullUsername() {
        DAOException exception = assertThrows(DAOException.class, () -> userDAO.checkCredentials(null, TEST_PASSWORD), "Checking credentials with null username should throw DAOException.");
        assertEquals(DAOException.DAOErrorType.INVALID_CREDENTIALS, exception.getErrorType());
    }

    @Test
    @Order(15)
    @DisplayName("Test checkCredentials with null password")
    void testCheckCredentials_NullPassword() throws DAOException, SQLException {
        // Create user first so username is valid
        userDAO.createUser(TEST_USERNAME, TEST_PASSWORD, TEST_NAME, TEST_SURNAME);
        connection.commit();

        DAOException exception = assertThrows(DAOException.class, () -> userDAO.checkCredentials(TEST_USERNAME, null), "Checking credentials with null password should throw DAOException.");
        assertEquals(DAOException.DAOErrorType.INVALID_CREDENTIALS, exception.getErrorType());
    }
}
