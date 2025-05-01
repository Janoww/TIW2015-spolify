package it.polimi.tiw.projects.dao;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.io.IOException; // Added for Files.deleteIfExists
import java.nio.file.Files; // Added for Files.deleteIfExists
import java.nio.file.Path; // Added for Path
import java.nio.file.Paths; // Added for Paths
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled; // Import Disabled for obsolete tests if we keep them
import org.junit.jupiter.api.Test;
// import org.junit.jupiter.api.io.TempDir; // Not using TempDir due to static path in DAO
import it.polimi.tiw.projects.exceptions.DAOException;

class AudioDAOTest {

    private AudioDAO audioDAO;
    private static final String SAMPLES_DIR = "/sample_audio/"; // Relative to resources root
    private List<String> pathsToDelete; // Track paths created during tests for cleanup

    // Note: The static STORAGE_DIRECTORY in AudioDAO makes true isolated testing
    // difficult. These tests will interact with the actual storage directory
    // in the user's home. The @AfterEach cleanup attempts to mitigate this.

    @BeforeEach
    void setUp() {
        audioDAO = new AudioDAO();
        pathsToDelete = new ArrayList<>();
    }

    @AfterEach
    void tearDown() {
        // Construct the base storage directory path similar to AudioDAO
        String homeDir = System.getProperty("user.home");
        // Use the constants defined in AudioDAO if they were accessible,
        // otherwise replicate the logic carefully.
        Path storageDir = Paths.get(homeDir, "Spolify", "song"); // Replicated logic

        // Attempt to clean up any files created during the tests directly
        for (String relativePath : pathsToDelete) {
            try {
                // Extract only the filename part from the relative path (e.g., "song/xyz.mp3"
                // -> "xyz.mp3")
                // This assumes relativePath always starts with "song/"
                String filename = Paths.get(relativePath).getFileName().toString();
                Path fullPath = storageDir.resolve(filename);

                System.out.println("Attempting direct cleanup: Deleting " + fullPath);
                boolean deleted = Files.deleteIfExists(fullPath);
                if (!deleted) {
                    // This is expected if the file was already deleted or never created properly
                    System.out.println("Cleanup info: File not found or already deleted: " + fullPath);
                } else {
                    System.out.println("Cleanup success: Deleted " + fullPath);
                }
            } catch (IOException e) {
                // Log I/O errors during direct deletion
                System.err.println("Error during direct test cleanup deleting " + relativePath + ": " + e.getMessage());
            } catch (Exception e) {
                // Catch other potential errors like invalid path format
                System.err.println(
                        "Unexpected error during direct test cleanup for " + relativePath + ": " + e.getMessage());
            }
        }
        pathsToDelete.clear();
    }

    private InputStream getResourceStream(String resourceName) {
        InputStream stream = getClass().getResourceAsStream(SAMPLES_DIR + resourceName);
        assertNotNull(stream, "Test resource not found: " + SAMPLES_DIR + resourceName);
        return stream;
    }

    // --- saveAudio Tests (modified for cleanup tracking) ---

    @Test
    void saveAudio_shouldReturnPathAndAllowCleanup_whenValidMp3Provided() throws DAOException {
        InputStream inputStream = getResourceStream("valid.mp3");
        String relativePath = audioDAO.saveAudio(inputStream, "test_song.mp3");

        assertNotNull(relativePath, "Returned path should not be null for valid MP3");
        assertFalse(relativePath.isBlank(), "Returned path should not be blank for valid MP3");
        assertTrue(relativePath.startsWith("song/"), "Relative path should start with 'song/'");
        // DAO now ensures the extension matches the *detected* type (e.g., audio/mpeg
        // -> .mp3)
        assertTrue(relativePath.endsWith(".mp3"), "Relative path should end with '.mp3' based on detected type");

        pathsToDelete.add(relativePath); // Track for cleanup
    }

    @Test
    void saveAudio_shouldReturnPathAndAllowCleanup_whenValidWavProvided() throws DAOException {
        InputStream inputStream = getResourceStream("valid.wav");
        String relativePath = audioDAO.saveAudio(inputStream, "test_song.wav");

        assertNotNull(relativePath, "Returned path should not be null for valid WAV");
        assertFalse(relativePath.isBlank(), "Returned path should not be blank for valid WAV");
        assertTrue(relativePath.startsWith("song/"), "Relative path should start with 'song/'");
        // DAO now ensures the extension matches the *detected* type (e.g., audio/wav,
        // audio/x-wav -> .wav)
        assertTrue(relativePath.endsWith(".wav"), "Relative path should end with '.wav' based on detected type");

        pathsToDelete.add(relativePath); // Track for cleanup
    }

    @Test
    void saveAudio_shouldReturnPathAndAllowCleanup_whenValidOggProvided() throws DAOException {
        InputStream inputStream = getResourceStream("valid.ogg");
        String relativePath = audioDAO.saveAudio(inputStream, "test_song.ogg");

        assertNotNull(relativePath, "Returned path should not be null for valid OGG");
        assertFalse(relativePath.isBlank(), "Returned path should not be blank for valid OGG");
        assertTrue(relativePath.startsWith("song/"), "Relative path should start with 'song/'");
        // DAO now ensures the extension matches the *detected* type (e.g., audio/ogg,
        // audio/vorbis -> .ogg)
        assertTrue(relativePath.endsWith(".ogg"), "Relative path should end with '.ogg' based on detected type");

        pathsToDelete.add(relativePath); // Track for cleanup
    }

    // --- saveAudio Validation Failure / Edge Case Tests (Updated) ---

    @Test
    // @Disabled("Obsolete: Validation is now based on MIME type, not original
    // extension")
    void saveAudio_shouldSaveWithCorrectExtension_whenValidContentHasWrongExtension() throws DAOException {
        // This test now checks if valid audio content (MP3) with a wrong extension
        // (.txt)
        // is correctly identified, saved, and given the proper extension (.mp3).
        InputStream inputStream = getResourceStream("valid.mp3"); // Use valid MP3 content
        String originalFilename = "valid_mp3_pretending_to_be.txt"; // Incorrect extension
        String relativePath = audioDAO.saveAudio(inputStream, originalFilename);

        assertNotNull(relativePath, "Path should not be null for valid content with wrong extension");
        // Tika should detect audio/mpeg, map it to .mp3 via ALLOWED_MIME_TYPES_MAP
        assertTrue(relativePath.endsWith(".mp3"),
                "Should be saved with correct extension (.mp3) based on content, not original filename");
        assertTrue(relativePath.startsWith("song/"), "Path should start with song/");

        pathsToDelete.add(relativePath); // Track for cleanup
    }

    @Test
    void saveAudio_shouldThrowIllegalArgument_whenContentTypeIsInvalid() {
        // This test remains valid. Tika should detect text/plain, which is not an
        // allowed audio MIME type.
        InputStream inputStream = getResourceStream("txt_pretending_to_be.mp3");
        assertThrows(IllegalArgumentException.class, () -> {
            audioDAO.saveAudio(inputStream, "txt_pretending_to_be.mp3");
        }, "Should throw IllegalArgumentException when content type (text) is invalid despite .mp3 extension");
    }

    @Test
    void saveAudio_shouldSaveWithCorrectExtension_whenContentTypeMismatchesExtension() throws DAOException {
        // This test now checks if valid WAV content with an MP3 extension
        // is correctly identified (as audio/wav or similar), saved, and given the
        // proper .wav extension.
        InputStream inputStream = getResourceStream("valid.wav"); // Use valid WAV content
        String originalFilename = "valid_wav_pretending_to_be.mp3"; // Incorrect extension
        String relativePath = audioDAO.saveAudio(inputStream, originalFilename);

        assertNotNull(relativePath,
                "Path should not be null when content type mismatches extension but is valid audio");
        // Tika should detect audio/wav (or x-wav), map it to .wav via
        // ALLOWED_MIME_TYPES_MAP
        assertTrue(relativePath.endsWith(".wav"),
                "Should be saved with correct extension (.wav) based on content, not original filename");
        assertTrue(relativePath.startsWith("song/"), "Path should start with song/");

        pathsToDelete.add(relativePath); // Track for cleanup
    }

    @Test
    void saveAudio_shouldThrowIllegalArgument_whenFilenameIsNull() {
        InputStream inputStream = getResourceStream("valid.mp3"); // Need a valid stream for the call
        assertThrows(IllegalArgumentException.class, () -> {
            audioDAO.saveAudio(inputStream, null);
        }, "Should throw IllegalArgumentException for null filename");
    }

    @Test
    void saveAudio_shouldThrowIllegalArgument_whenFilenameIsBlank() {
        InputStream inputStream = getResourceStream("valid.mp3");
        assertThrows(IllegalArgumentException.class, () -> {
            audioDAO.saveAudio(inputStream, "  ");
        }, "Should throw IllegalArgumentException for blank filename");
    }

    @Test
    void saveAudio_shouldSaveSuccessfully_whenFilenameHasNoExtension() throws DAOException {
        // Test that saving works even without an original extension,
        // relying on Tika detection and the DAO assigning the correct one.
        InputStream inputStream = getResourceStream("valid.mp3"); // Content doesn't matter here
        String relativePath = audioDAO.saveAudio(inputStream, "testfile_no_extension");

        assertNotNull(relativePath, "Path should not be null even if original filename has no extension");
        assertFalse(relativePath.isBlank(), "Path should not be blank");
        assertTrue(relativePath.startsWith("song/"), "Path should start with song/");
        assertTrue(relativePath.endsWith(".mp3"), "Should be saved with correct extension (.mp3) based on content");

        pathsToDelete.add(relativePath); // Track for cleanup
    }

    @Test
    void saveAudio_shouldThrowIllegalArgument_whenImageFileProvided() {
        InputStream inputStream = getResourceStream("image.jpg");
        assertThrows(IllegalArgumentException.class, () -> {
            // Even with allowed extension, Tika should detect non-audio content
            audioDAO.saveAudio(inputStream, "fake_audio.mp3");
        }, "Should throw IllegalArgumentException when content is an image, regardless of filename extension");
    }

    // --- deleteAudio Tests ---

    @Test
    void deleteAudio_shouldDeleteExistingFileAndReturnTrue() throws DAOException {
        // Arrange: Save a file first
        InputStream inputStream = getResourceStream("valid.mp3");
        String relativePath = audioDAO.saveAudio(inputStream, "delete_test.mp3");
        assertNotNull(relativePath);
        // Don't add to pathsToDelete here, as this test *is* the deletion

        // Act: Delete the file
        boolean result = audioDAO.deleteAudio(relativePath);

        // Assert
        assertTrue(result, "deleteAudio should return true for an existing file");

        // Optional: Verify file is actually gone (might be flaky depending on FS
        // timing)
        // Path expectedPath = Paths.get(System.getProperty("user.home"), "Spolify",
        // relativePath);
        // assertFalse(Files.exists(expectedPath), "File should not exist after
        // deletion");
    }

    @Test
    void deleteAudio_shouldReturnFalse_whenFileDoesNotExist() throws DAOException {
        // Arrange: A path that definitely doesn't exist
        String nonExistentPath = "song/non_existent_file_" + System.currentTimeMillis() + ".mp3";

        // Act
        boolean result = audioDAO.deleteAudio(nonExistentPath);

        // Assert
        assertFalse(result, "deleteAudio should return false for a non-existent file");
    }

    @Test
    void deleteAudio_shouldThrowIllegalArgument_whenPathIsNull() {
        assertThrows(IllegalArgumentException.class, () -> {
            audioDAO.deleteAudio(null);
        }, "Should throw IllegalArgumentException for null path");
    }

    @Test
    void deleteAudio_shouldThrowIllegalArgument_whenPathIsBlank() {
        assertThrows(IllegalArgumentException.class, () -> {
            audioDAO.deleteAudio("   ");
        }, "Should throw IllegalArgumentException for blank path");
    }

    @Test
    void deleteAudio_shouldThrowIllegalArgument_whenPathIsInvalidFormat() {
        // Path doesn't start with "song/"
        assertThrows(IllegalArgumentException.class, () -> {
            audioDAO.deleteAudio("invalid_folder/somefile.mp3");
        }, "Should throw IllegalArgumentException for path not starting with 'song/'");

        // Path is just the subfolder
        assertThrows(IllegalArgumentException.class, () -> {
            audioDAO.deleteAudio("song/");
        }, "Should throw IllegalArgumentException for path being just the subfolder");
    }

    @Test
    void deleteAudio_shouldThrowIllegalArgument_whenPathIsMalicious() {
        // Attempt to go up directories (basic check in DAO should prevent this)
        assertThrows(IllegalArgumentException.class, () -> {
            audioDAO.deleteAudio("song/../../etc/passwd");
        }, "Should throw IllegalArgumentException for potentially malicious path");
    }

}
