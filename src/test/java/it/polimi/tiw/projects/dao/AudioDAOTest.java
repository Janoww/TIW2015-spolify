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
// Removed ArrayList and List imports as pathsToDelete is removed
// import java.util.ArrayList;
// import java.util.List;

// Removed AfterEach import as tearDown is removed
// import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled; // Import Disabled for obsolete tests if we keep them
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir; // Import TempDir for temporary test directories
import it.polimi.tiw.projects.exceptions.DAOException;

class AudioDAOTest {

    private AudioDAO audioDAO;
    private static final String SAMPLES_DIR = "/sample_audio/"; // Relative to resources root
    private static final String AUDIO_SUBFOLDER = "song"; // Replicate DAO's internal subfolder name for path checks
    // Removed pathsToDelete list

    // Inject a temporary directory for each test method
    @TempDir
    Path tempDir;

    // Note: Tests now run against a temporary directory provided by @TempDir,
    // ensuring isolation and automatic cleanup.

    @BeforeEach
    void setUp() {
        // Instantiate DAO with the temporary directory path
        audioDAO = new AudioDAO(tempDir);
        // Removed pathsToDelete initialization
    }

    // Removed tearDown method as @TempDir handles cleanup

    private InputStream getResourceStream(String resourceName) {
        InputStream stream = getClass().getResourceAsStream(SAMPLES_DIR + resourceName);
        assertNotNull(stream, "Test resource not found: " + SAMPLES_DIR + resourceName);
        return stream;
    }

    // --- saveAudio Tests (modified for cleanup tracking) ---

    @Test
    void saveAudio_shouldReturnFilename_whenValidMp3Provided() throws DAOException {
        InputStream inputStream = getResourceStream("valid.mp3");
        String filename = audioDAO.saveAudio(inputStream, "test_song.mp3");

        assertNotNull(filename, "Returned filename should not be null for valid MP3");
        assertFalse(filename.isBlank(), "Returned filename should not be blank for valid MP3");
        assertFalse(filename.contains("/"), "Filename should not contain path separators");
        assertFalse(filename.contains("\\"), "Filename should not contain path separators");
        // DAO now ensures the extension matches the *detected* type (e.g., audio/mpeg
        // -> .mp3)
        assertTrue(filename.endsWith(".mp3"), "Filename should end with '.mp3' based on detected type");
        // Check if file exists in the temp directory
        assertTrue(Files.exists(tempDir.resolve(AUDIO_SUBFOLDER).resolve(filename)), "File should exist in temp dir");
        // Removed pathsToDelete tracking
    }

    @Test
    void saveAudio_shouldReturnFilename_whenValidWavProvided() throws DAOException {
        InputStream inputStream = getResourceStream("valid.wav");
        String filename = audioDAO.saveAudio(inputStream, "test_song.wav");

        assertNotNull(filename, "Returned filename should not be null for valid WAV");
        assertFalse(filename.isBlank(), "Returned filename should not be blank for valid WAV");
        assertFalse(filename.contains("/"), "Filename should not contain path separators");
        assertFalse(filename.contains("\\"), "Filename should not contain path separators");
        // DAO now ensures the extension matches the *detected* type (e.g., audio/wav,
        // audio/x-wav -> .wav)
        assertTrue(filename.endsWith(".wav"), "Filename should end with '.wav' based on detected type");
        assertTrue(Files.exists(tempDir.resolve(AUDIO_SUBFOLDER).resolve(filename)), "File should exist in temp dir");
        // Removed pathsToDelete tracking
    }

    @Test
    void saveAudio_shouldReturnFilename_whenValidOggProvided() throws DAOException {
        InputStream inputStream = getResourceStream("valid.ogg");
        String filename = audioDAO.saveAudio(inputStream, "test_song.ogg");

        assertNotNull(filename, "Returned filename should not be null for valid OGG");
        assertFalse(filename.isBlank(), "Returned filename should not be blank for valid OGG");
        assertFalse(filename.contains("/"), "Filename should not contain path separators");
        assertFalse(filename.contains("\\"), "Filename should not contain path separators");
        // DAO now ensures the extension matches the *detected* type (e.g., audio/ogg,
        // audio/vorbis -> .ogg)
        assertTrue(filename.endsWith(".ogg"), "Filename should end with '.ogg' based on detected type");
        assertTrue(Files.exists(tempDir.resolve(AUDIO_SUBFOLDER).resolve(filename)), "File should exist in temp dir");
        // Removed pathsToDelete tracking
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
        String filename = audioDAO.saveAudio(inputStream, originalFilename);

        assertNotNull(filename, "Filename should not be null for valid content with wrong extension");
        // Tika should detect audio/mpeg, map it to .mp3 via ALLOWED_MIME_TYPES_MAP
        assertTrue(filename.endsWith(".mp3"),
                "Should be saved with correct extension (.mp3) based on content, not original filename");
        assertFalse(filename.contains("/"), "Filename should not contain path separators");
        assertTrue(Files.exists(tempDir.resolve(AUDIO_SUBFOLDER).resolve(filename)), "File should exist in temp dir");

        // Removed pathsToDelete tracking
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
        String filename = audioDAO.saveAudio(inputStream, originalFilename);

        assertNotNull(filename,
                "Filename should not be null when content type mismatches extension but is valid audio");
        // Tika should detect audio/wav (or x-wav), map it to .wav via
        // ALLOWED_MIME_TYPES_MAP
        assertTrue(filename.endsWith(".wav"),
                "Should be saved with correct extension (.wav) based on content, not original filename");
        assertFalse(filename.contains("/"), "Filename should not contain path separators");
        assertTrue(Files.exists(tempDir.resolve(AUDIO_SUBFOLDER).resolve(filename)), "File should exist in temp dir");

        // Removed pathsToDelete tracking
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
        InputStream inputStream = getResourceStream("valid.mp3"); // Content type matters here (mp3)
        String filename = audioDAO.saveAudio(inputStream, "testfile_no_extension");

        assertNotNull(filename, "Filename should not be null even if original filename has no extension");
        assertFalse(filename.isBlank(), "Filename should not be blank");
        assertFalse(filename.contains("/"), "Filename should not contain path separators");
        assertTrue(filename.endsWith(".mp3"), "Should be saved with correct extension (.mp3) based on content");
        assertTrue(Files.exists(tempDir.resolve(AUDIO_SUBFOLDER).resolve(filename)), "File should exist in temp dir");

        // Removed pathsToDelete tracking
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
    void deleteAudio_shouldDeleteExistingFileAndReturnTrue() throws DAOException, IOException {
        // Arrange: Save a file first
        InputStream inputStream = getResourceStream("valid.mp3");
        String filename = audioDAO.saveAudio(inputStream, "delete_test.mp3");
        // No need to assertNotNull, saveAudio throws if it fails badly
        Path expectedPath = tempDir.resolve(AUDIO_SUBFOLDER).resolve(filename);
        assertTrue(Files.exists(expectedPath), "File should exist after saving");

        // Act: Delete the file using the filename
        boolean result = audioDAO.deleteAudio(filename);

        // Assert
        assertTrue(result, "deleteAudio should return true for an existing file");
        assertFalse(Files.exists(expectedPath), "File should not exist after deletion");
    }

    @Test
    void deleteAudio_shouldReturnFalse_whenFileDoesNotExist() throws DAOException {
        // Arrange: A filename that definitely doesn't exist in the temp dir
        String nonExistentFilename = "non_existent_file_" + System.currentTimeMillis() + ".mp3";

        // Act
        boolean result = audioDAO.deleteAudio(nonExistentFilename);

        // Assert
        assertFalse(result, "deleteAudio should return false for a non-existent filename");
    }

    @Test
    void deleteAudio_shouldThrowIllegalArgument_whenFilenameIsNull() {
        assertThrows(IllegalArgumentException.class, () -> {
            audioDAO.deleteAudio(null);
        }, "Should throw IllegalArgumentException for null filename");
    }

    @Test
    void deleteAudio_shouldThrowIllegalArgument_whenFilenameIsBlank() {
        assertThrows(IllegalArgumentException.class, () -> {
            audioDAO.deleteAudio("   ");
        }, "Should throw IllegalArgumentException for blank filename");
    }

    @Test
    void deleteAudio_shouldThrowIllegalArgument_whenFilenameIsInvalidFormat() {
        // Filename contains forward slash
        assertThrows(IllegalArgumentException.class, () -> {
            audioDAO.deleteAudio("invalid/name.mp3");
        }, "Should throw IllegalArgumentException for filename containing '/'");

        // Filename contains backslash
        assertThrows(IllegalArgumentException.class, () -> {
            audioDAO.deleteAudio("invalid\\name.mp3");
        }, "Should throw IllegalArgumentException for filename containing '\\'");

        // Filename is just "."
        assertThrows(IllegalArgumentException.class, () -> {
            audioDAO.deleteAudio(".");
        }, "Should throw IllegalArgumentException for filename being '.'");
    }

    @Test
    void deleteAudio_shouldThrowIllegalArgument_whenFilenameContainsTraversal() {
        // Attempt to go up directories
        assertThrows(IllegalArgumentException.class, () -> {
            audioDAO.deleteAudio("../../../etc/passwd");
        }, "Should throw IllegalArgumentException for filename containing '..'");
    }

}
