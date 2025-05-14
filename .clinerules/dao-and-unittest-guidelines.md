## Brief overview

These guidelines are derived from the existing DAO and unit test implementations in the project. They aim to ensure consistency and robustness in data access and testing.

## DAO Design Principles

- **Connection Management:**
  - DAOs should receive a `java.sql.Connection` object via constructor injection.
  - Example: `public AlbumDAO(Connection connection) { this.connection = connection; }`
- **Error Handling:**
  - Use the custom `it.polimi.tiw.projects.exceptions.DAOException` for all data access related errors.
  - Specify the appropriate `DAOException.DAOErrorType` to categorize the error.
  - Example: `throw new DAOException("Album not found", DAOErrorType.NOT_FOUND);`
- **Logging:**
  - Utilize SLF4J for logging. Get a logger instance per class: `private static final Logger logger = LoggerFactory.getLogger(MyDAO.class);`
  - Log important operations, parameters, errors, and debug information.
  - Example: `logger.debug("Attempting to find album by ID: {}", idAlbum);`
- **SQL Operations:**
  - Prefer `PreparedStatement` over `Statement` to prevent SQL injection and for better performance with parameterized queries.
  - Always close `ResultSet` and `PreparedStatement` objects in a `try-with-resources` block or a `finally` block.
  - Example: `try (PreparedStatement pStatement = connection.prepareStatement(query)) { ... }`
- **UUID Handling:**
  - When storing UUIDs in the database, use `UUID_TO_BIN(?)`.
  - When retrieving UUIDs from the database, use `BIN_TO_UUID(column_name)`.
  - Convert string representations to `UUID` objects using `UUID.fromString()`.
- **File Handling DAOs (e.g., AudioDAO, ImageDAO):**
  - Store files in dedicated subfolders (e.g., `baseStorageDirectory.resolve("song")`).
  - Validate file content using Apache Tika before saving. Maintain a map of allowed MIME types and their corresponding extensions.
  - Generate unique, sanitized filenames (e.g., by appending a UUID and using a canonical extension based on MIME type).
  - Implement robust file deletion logic, including path validation to prevent traversal attacks and ensure deletion within the designated storage directory.
- **Transactions:**
  - For operations involving multiple database modifications that must be atomic (e.g., creating a playlist and adding its songs), manage transactions manually:
    - `connection.setAutoCommit(false);`
    - `connection.commit();` on success.
    - `connection.rollback();` on failure.
    - Restore `autoCommit` state in a `finally` block.
  - Example: `PlaylistDAO.createPlaylist()`

## Unit Testing (JUnit 5)

- **Test Structure:**

  - Use `@TestInstance(TestInstance.Lifecycle.PER_CLASS)` and `@TestMethodOrder(MethodOrderer.OrderAnnotation.class)`.
  - Manage database connection in `@BeforeAll` (create) and `@AfterAll` (close).
  - Set `connection.setAutoCommit(false);` in `@BeforeAll`.

- **Data Management:**
  - Create necessary test data (users, parent entities) in `@BeforeAll` or `@BeforeEach`.
  - Implement thorough cleanup methods in `@AfterAll` and/or `@AfterEach` to remove test data. Ensure cleanup order respects foreign key constraints.
  - Use unique names for test entities (e.g., by appending `System.currentTimeMillis()` or a UUID) to avoid conflicts.
  - Commit changes after setup or successful operations within a test. Rollback changes in `@AfterEach` to isolate tests.
- **Test Cases:**
  - Cover successful ("happy path") scenarios.
  - Test for expected `DAOException`s with correct `DAOErrorType` for failure scenarios (e.g., duplicate name, item not found, unauthorized access, foreign key violations).
  - Verify the state of the database directly using SQL queries within tests if necessary, especially to confirm data before and after commits/rollbacks.
  - For file DAOs, use `@TempDir` to create temporary directories for file operations, ensuring tests are isolated and cleanup is automatic.
- **Assertions:**
  - Use appropriate JUnit 5 assertions: `assertEquals`, `assertNotNull`, `assertTrue`, `assertFalse`, `assertThrows`, `assertDoesNotThrow`.
- **Logging in Tests:**
  - Use SLF4J for logging within test classes for better traceability of test execution.
- **Example Test Flow (Conceptual):**

  ```java

  @Test
  @Order(1)
  void testCreateEntity_Success() throws DAOException, SQLException {
      // 1. DAO call to create entity
      Entity createdEntity = entityDAO.createEntity(...);
      // 2. Assertions on the returned entity
      assertNotNull(createdEntity);
      // 3. (Optional) Direct DB check before commit
      Entity foundInDbBeforeCommit = findEntityByIdDirectly(createdEntity.getId());
      assertNotNull(foundInDbBeforeCommit);
      // 4. Commit transaction
      connection.commit();
      // 5. (Optional) Direct DB check after commit
      Entity foundInDbAfterCommit = findEntityByIdDirectly(createdEntity.getId());
      assertNotNull(foundInDbAfterCommit);
  }
  ```

## General Coding Style

- **Clarity and Readability:** Write clear, well-commented code.
- **Constants:** Define constants for recurring string literals or magic numbers (e.g., `DB_URL`, `TEST_USERNAME`).
- **Exception Messages:** Provide informative messages in exceptions.
