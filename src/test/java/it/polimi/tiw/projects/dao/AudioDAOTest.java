package it.polimi.tiw.projects.dao;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import it.polimi.tiw.projects.beans.FileData;
import it.polimi.tiw.projects.exceptions.DAOException;
import org.apache.tika.Tika;

class AudioDAOTest {

    private AudioDAO audioDAO;
    private Tika tika;
    private static final String SAMPLES_DIR = "/sample_audio/";
    private static final String AUDIO_SUBFOLDER = "song";

    @TempDir
    Path tempDir;

    // Note: Tests now run against a temporary directory provided by @TempDir,
    // ensuring isolation and automatic cleanup.

    @BeforeEach
    void setUp() {
        // Instantiate DAO with the temporary directory path
        audioDAO = new AudioDAO(tempDir);
        tika = new Tika();
    }

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

        assertTrue(filename.endsWith(".mp3"), "Filename should end with '.mp3' based on detected type");

        assertTrue(Files.exists(tempDir.resolve(AUDIO_SUBFOLDER).resolve(filename)), "File should exist in temp dir");
    }

    @Test
    void saveAudio_shouldReturnFilename_whenValidWavProvided() throws DAOException {
        InputStream inputStream = getResourceStream("valid.wav");
        String filename = audioDAO.saveAudio(inputStream, "test_song.wav");

        assertNotNull(filename, "Returned filename should not be null for valid WAV");
        assertFalse(filename.isBlank(), "Returned filename should not be blank for valid WAV");
        assertFalse(filename.contains("/"), "Filename should not contain path separators");
        assertFalse(filename.contains("\\"), "Filename should not contain path separators");

        assertTrue(filename.endsWith(".wav"), "Filename should end with '.wav' based on detected type");
        assertTrue(Files.exists(tempDir.resolve(AUDIO_SUBFOLDER).resolve(filename)), "File should exist in temp dir");

    }

    @Test
    void saveAudio_shouldReturnFilename_whenValidOggProvided() throws DAOException {
        InputStream inputStream = getResourceStream("valid.ogg");
        String filename = audioDAO.saveAudio(inputStream, "test_song.ogg");

        assertNotNull(filename, "Returned filename should not be null for valid OGG");
        assertFalse(filename.isBlank(), "Returned filename should not be blank for valid OGG");
        assertFalse(filename.contains("/"), "Filename should not contain path separators");
        assertFalse(filename.contains("\\"), "Filename should not contain path separators");

        assertTrue(filename.endsWith(".ogg"), "Filename should end with '.ogg' based on detected type");
        assertTrue(Files.exists(tempDir.resolve(AUDIO_SUBFOLDER).resolve(filename)), "File should exist in temp dir");

    }

    // --- saveAudio Validation Failure / Edge Case Tests (Updated) ---

    @Test
    // @Disabled("Obsolete: Validation is now based on MIME type, not original
    // extension")
    void saveAudio_shouldSaveWithCorrectExtension_whenValidContentHasWrongExtension() throws DAOException {
        // This test now checks if valid audio content (MP3) with a wrong extension
        // (.txt)
        // is correctly identified, saved, and given the proper extension (.mp3).
        InputStream inputStream = getResourceStream("valid.mp3");
        String originalFilename = "valid_mp3_pretending_to_be.txt";
        String filename = audioDAO.saveAudio(inputStream, originalFilename);

        assertNotNull(filename, "Filename should not be null for valid content with wrong extension");
        // Tika should detect audio/mpeg, map it to .mp3 via ALLOWED_MIME_TYPES_MAP
        assertTrue(filename.endsWith(".mp3"),
                "Should be saved with correct extension (.mp3) based on content, not original filename");
        assertFalse(filename.contains("/"), "Filename should not contain path separators");
        assertTrue(Files.exists(tempDir.resolve(AUDIO_SUBFOLDER).resolve(filename)), "File should exist in temp dir");
    }

    @Test
    void saveAudio_shouldThrowIllegalArgument_whenContentTypeIsInvalid() {
        InputStream inputStream = getResourceStream("txt_pretending_to_be.mp3");
        assertThrows(IllegalArgumentException.class, () -> {
            audioDAO.saveAudio(inputStream, "txt_pretending_to_be.mp3");
        }, "Should throw IllegalArgumentException when content type (text) is invalid despite .mp3 extension");
    }

    @Test
    void saveAudio_shouldSaveWithCorrectExtension_whenContentTypeMismatchesExtension() throws DAOException {
        InputStream inputStream = getResourceStream("valid.wav");
        String originalFilename = "valid_wav_pretending_to_be.mp3";
        String filename = audioDAO.saveAudio(inputStream, originalFilename);

        assertNotNull(filename,
                "Filename should not be null when content type mismatches extension but is valid audio");

        assertTrue(filename.endsWith(".wav"),
                "Should be saved with correct extension (.wav) based on content, not original filename");
        assertFalse(filename.contains("/"), "Filename should not contain path separators");
        assertTrue(Files.exists(tempDir.resolve(AUDIO_SUBFOLDER).resolve(filename)), "File should exist in temp dir");
    }

    @Test
    void saveAudio_shouldThrowIllegalArgument_whenFilenameIsNull() {
        InputStream inputStream = getResourceStream("valid.mp3");
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
        InputStream inputStream = getResourceStream("valid.mp3");
        String filename = audioDAO.saveAudio(inputStream, "testfile_no_extension");

        assertNotNull(filename, "Filename should not be null even if original filename has no extension");
        assertFalse(filename.isBlank(), "Filename should not be blank");
        assertFalse(filename.contains("/"), "Filename should not contain path separators");
        assertTrue(filename.endsWith(".mp3"), "Should be saved with correct extension (.mp3) based on content");
        assertTrue(Files.exists(tempDir.resolve(AUDIO_SUBFOLDER).resolve(filename)), "File should exist in temp dir");
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
    void deleteAudio_shouldDeleteExistingFile_whenFileExists() throws DAOException, IOException {
        // Arrange: Save a file first
        InputStream inputStream = getResourceStream("valid.mp3");
        String filename = audioDAO.saveAudio(inputStream, "delete_test.mp3");

        Path expectedPath = tempDir.resolve(AUDIO_SUBFOLDER).resolve(filename);
        assertTrue(Files.exists(expectedPath), "File should exist after saving");

        // Act & Assert: Delete the file using the filename and assert no exception
        assertDoesNotThrow(() -> audioDAO.deleteAudio(filename), "Deleting an existing file should not throw");
        assertFalse(Files.exists(expectedPath), "File should not exist after deletion");
    }

    @Test
    void deleteAudio_shouldThrowNotFound_whenFileDoesNotExist() {
        // Arrange: A filename that definitely doesn't exist in the temp dir
        String nonExistentFilename = "non_existent_file_" + System.currentTimeMillis() + ".mp3";

        // Act & Assert: Expect DAOException with NOT_FOUND type
        DAOException exception = assertThrows(DAOException.class, () -> {
            audioDAO.deleteAudio(nonExistentFilename);
        }, "deleteAudio should throw DAOException for a non-existent filename");
        assertEquals(DAOException.DAOErrorType.NOT_FOUND, exception.getErrorType(),
                "Exception type should be NOT_FOUND");
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

    // --- getAudio Tests ---

    @Test
    void getAudio_shouldReturnFileData_whenFileExists() throws DAOException, IOException {
        // Arrange: Save a file first
        String originalTestFileName = "get_test.mp3";
        InputStream inputStream = getResourceStream("valid.mp3");
        String savedFilename = audioDAO.saveAudio(inputStream, originalTestFileName);
        Path expectedPath = tempDir.resolve(AUDIO_SUBFOLDER).resolve(savedFilename);
        assertTrue(Files.exists(expectedPath), "File should exist after saving for get test");

        // Act: Retrieve the file
        try (FileData fileData = audioDAO.getAudio(savedFilename)) {
            // Assert
            assertNotNull(fileData, "FileData should not be null for existing file");
            assertEquals(savedFilename, fileData.getFilename(), "Filename in FileData should match");

            // Verify MIME type (Tika might give slightly different but compatible types)
            String expectedMimeType = tika.detect(expectedPath);
            assertEquals(expectedMimeType, fileData.getMimeType(), "MIME type should match detected type");

            // Verify size
            long expectedSize = Files.size(expectedPath);
            assertEquals(expectedSize, fileData.getSize(), "File size should match");

            // Verify content (read stream and compare)
            assertNotNull(fileData.getContent(), "Content stream should not be null");
            byte[] originalBytes = Files.readAllBytes(expectedPath);
            ByteArrayOutputStream retrievedBytesStream = new ByteArrayOutputStream();
            fileData.getContent().transferTo(retrievedBytesStream);
            assertArrayEquals(originalBytes, retrievedBytesStream.toByteArray(), "File content should match");
        }
    }

    @Test
    void getAudio_shouldThrowNotFound_whenFileDoesNotExist() {
        String nonExistentFilename = "non_existent_for_get.mp3";
        DAOException exception = assertThrows(DAOException.class, () -> {
            audioDAO.getAudio(nonExistentFilename);
        }, "getAudio should throw DAOException for a non-existent filename");
        assertEquals(DAOException.DAOErrorType.NOT_FOUND, exception.getErrorType(),
                "Exception type should be NOT_FOUND for non-existent file");
    }

    @Test
    void getAudio_shouldThrowIllegalArgument_whenFilenameIsNull() {
        assertThrows(IllegalArgumentException.class, () -> {
            audioDAO.getAudio(null);
        }, "Should throw IllegalArgumentException for null filename in getAudio");
    }

    @Test
    void getAudio_shouldThrowIllegalArgument_whenFilenameIsBlank() {
        assertThrows(IllegalArgumentException.class, () -> {
            audioDAO.getAudio("   ");
        }, "Should throw IllegalArgumentException for blank filename in getAudio");
    }

    @Test
    void getAudio_shouldThrowIllegalArgument_whenFilenameContainsTraversal() {
        assertThrows(IllegalArgumentException.class, () -> {
            audioDAO.getAudio("../../../etc/passwd");
        }, "Should throw IllegalArgumentException for filename containing '..' in getAudio");
    }

    @Test
    void getAudio_shouldThrowIllegalArgument_whenFilenameIsInvalidFormat() {
        // Filename contains forward slash
        assertThrows(IllegalArgumentException.class, () -> {
            audioDAO.getAudio("invalid/name.mp3");
        }, "Should throw IllegalArgumentException for filename containing '/' in getAudio");

        // Filename contains backslash
        assertThrows(IllegalArgumentException.class, () -> {
            audioDAO.getAudio("invalid\\name.mp3");
        }, "Should throw IllegalArgumentException for filename containing '\\' in getAudio");
    }

    @Test
    void saveAudio_shouldThrowDAOException_whenInputStreamThrowsIOException() {
        // Create a mock InputStream that throws IOException on read
        InputStream mockInputStream = new InputStream() {
            @Override
            public int read() throws IOException {
                throw new IOException("Simulated InputStream read error");
            }

            @Override
            public int read(byte[] b, int off, int len) throws IOException {
                throw new IOException("Simulated InputStream read error");
            }
        };

        DAOException exception = assertThrows(DAOException.class, () -> {
            audioDAO.saveAudio(mockInputStream, "test_io_exception.mp3");
        }, "Should throw DAOException when InputStream has an I/O error");

        assertEquals(DAOException.DAOErrorType.GENERIC_ERROR, exception.getErrorType(),
                "DAOErrorType should be GENERIC_ERROR for I/O issues during save.");
        assertTrue(exception.getMessage().contains("Failed to save audio due to I/O error"),
                "Exception message should indicate I/O error during save.");
    }

    @Test
    void saveAudio_shouldTruncateAndSanitize_whenOriginalFilenameIsVeryLong() throws DAOException {
        InputStream inputStream = getResourceStream("valid.mp3");
        String veryLongName = "a".repeat(250) + "!@#$%^&*().mp3"; // Exceeds MAX_FILENAME_PREFIX_LENGTH
        String filename = audioDAO.saveAudio(inputStream, veryLongName);

        assertNotNull(filename);
        assertTrue(filename.endsWith(".mp3"));

        String prefix = filename.substring(0, filename.lastIndexOf('_'));
        assertTrue(prefix.length() <= 190,
                "Filename prefix should be truncated to MAX_FILENAME_PREFIX_LENGTH or less.");
        assertFalse(prefix.matches(".*[!@#$%^&*()].*"), "Sanitized prefix should not contain special characters.");
        assertTrue(Files.exists(tempDir.resolve(AUDIO_SUBFOLDER).resolve(filename)), "File should exist in temp dir");
    }

    @Test
    void saveAudio_shouldUseDefaultBaseName_whenSanitizedFilenameIsEmpty() throws DAOException {
        InputStream inputStream = getResourceStream("valid.ogg");
        String originalFilename = "!@#$%%^^&&**(()).ogg";
        String filename = audioDAO.saveAudio(inputStream, originalFilename);

        assertNotNull(filename);
        assertTrue(filename.endsWith(".ogg"));
        assertTrue(filename.startsWith("audio_"),
                "Filename should start with 'audio_' when original sanitizes to empty.");
        assertTrue(Files.exists(tempDir.resolve(AUDIO_SUBFOLDER).resolve(filename)), "File should exist in temp dir");
    }
}
