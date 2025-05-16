package it.polimi.tiw.projects.dao;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import it.polimi.tiw.projects.exceptions.DAOException;

class ImageDAOTest {

    private ImageDAO imageDAO;
    private static final String SAMPLES_DIR = "/sample_images/"; // Updated sample directory
    private static final String IMAGE_SUBFOLDER = "image"; // Matches ImageDAO subfolder

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        // Instantiate DAO with the temporary directory path
        imageDAO = new ImageDAO(tempDir);
    }

    private InputStream getResourceStream(String resourceName) {
        InputStream stream = getClass().getResourceAsStream(SAMPLES_DIR + resourceName);
        assertNotNull(stream, "Test resource not found: " + SAMPLES_DIR + resourceName);
        return stream;
    }

    // --- saveImage Tests ---

    @Test
    void saveImage_shouldReturnFilename_whenValidJpgProvided() throws DAOException {
        InputStream inputStream = getResourceStream("valid.jpg");
        String filename = imageDAO.saveImage(inputStream, "test_image.jpg");

        assertNotNull(filename, "Returned filename should not be null for valid JPG");
        assertFalse(filename.isBlank(), "Returned filename should not be blank for valid JPG");
        assertFalse(filename.contains("/"), "Filename should not contain path separators");
        assertFalse(filename.contains("\\"), "Filename should not contain path separators");

        // Tika detects image/jpeg, map should resolve to .jpg
        assertTrue(filename.endsWith(".jpg"),
                "Filename should end with '.jpg' based on detected type");
        assertTrue(Files.exists(tempDir.resolve(IMAGE_SUBFOLDER).resolve(filename)),
                "File should exist in temp dir");
    }

    @Test
    void saveImage_shouldReturnFilename_whenValidJpegProvided() throws DAOException {
        // Test with .jpeg extension, should still save as .jpg due to MIME type mapping
        InputStream inputStream = getResourceStream("valid.jpeg"); // Assuming you have a valid.jpeg
                                                                   // sample
        String filename = imageDAO.saveImage(inputStream, "test_image.jpeg");

        assertNotNull(filename, "Returned filename should not be null for valid JPEG");
        assertFalse(filename.isBlank(), "Returned filename should not be blank for valid JPEG");
        assertTrue(filename.endsWith(".jpg"),
                "Filename should end with '.jpg' based on detected type (image/jpeg)");
        assertTrue(Files.exists(tempDir.resolve(IMAGE_SUBFOLDER).resolve(filename)),
                "File should exist in temp dir");
    }

    @Test
    void saveImage_shouldReturnFilename_whenValidPngProvided() throws DAOException {
        InputStream inputStream = getResourceStream("valid.png");
        String filename = imageDAO.saveImage(inputStream, "test_image.png");

        assertNotNull(filename, "Returned filename should not be null for valid PNG");
        assertFalse(filename.isBlank(), "Returned filename should not be blank for valid PNG");
        assertTrue(filename.endsWith(".png"),
                "Filename should end with '.png' based on detected type");
        assertTrue(Files.exists(tempDir.resolve(IMAGE_SUBFOLDER).resolve(filename)),
                "File should exist in temp dir");
    }

    @Test
    void saveImage_shouldReturnFilename_whenValidWebpProvided() throws DAOException {
        InputStream inputStream = getResourceStream("valid.webp");
        String filename = imageDAO.saveImage(inputStream, "test_image.webp");

        assertNotNull(filename, "Returned filename should not be null for valid WEBP");
        assertFalse(filename.isBlank(), "Returned filename should not be blank for valid WEBP");
        assertTrue(filename.endsWith(".webp"),
                "Filename should end with '.webp' based on detected type");
        assertTrue(Files.exists(tempDir.resolve(IMAGE_SUBFOLDER).resolve(filename)),
                "File should exist in temp dir");
    }

    // --- saveImage Validation Failure / Edge Case Tests ---

    @Test
    void saveImage_shouldSaveWithCorrectExtension_whenValidContentHasWrongExtension()
            throws DAOException {
        // Valid JPG content pretending to be PNG
        InputStream inputStream = getResourceStream("valid.jpg");
        String originalFilename = "valid_jpg_pretending_to_be.png";
        String filename = imageDAO.saveImage(inputStream, originalFilename);

        assertNotNull(filename,
                "Filename should not be null for valid content with wrong extension");
        // Tika should detect image/jpeg, map it to .jpg
        assertTrue(filename.endsWith(".jpg"),
                "Should be saved with correct extension (.jpg) based on content, not original filename");
        assertTrue(Files.exists(tempDir.resolve(IMAGE_SUBFOLDER).resolve(filename)),
                "File should exist in temp dir");
    }

    @Test
    void saveImage_shouldThrowIllegalArgument_whenContentTypeIsInvalid() {
        // Text file pretending to be JPG
        InputStream inputStream = getResourceStream("text_pretending_to_be.jpg");
        assertThrows(IllegalArgumentException.class, () -> {
            imageDAO.saveImage(inputStream, "text_pretending_to_be.jpg");
        }, "Should throw IllegalArgumentException when content type (text) is invalid despite .jpg extension");
    }

    @Test
    void saveImage_shouldThrowIllegalArgument_whenAudioFileProvided() {
        // Audio file pretending to be JPG
        InputStream inputStream = getResourceStream("audio_pretending_to_be.jpg");
        assertThrows(IllegalArgumentException.class, () -> {
            imageDAO.saveImage(inputStream, "audio_pretending_to_be.jpg");
        }, "Should throw IllegalArgumentException when content is audio, regardless of filename extension");
    }

    @Test
    void saveImage_shouldThrowIllegalArgument_whenFilenameIsNull() {
        InputStream inputStream = getResourceStream("valid.jpg");
        assertThrows(IllegalArgumentException.class, () -> {
            imageDAO.saveImage(inputStream, null);
        }, "Should throw IllegalArgumentException for null filename");
    }

    @Test
    void saveImage_shouldThrowIllegalArgument_whenFilenameIsBlank() {
        InputStream inputStream = getResourceStream("valid.jpg");
        assertThrows(IllegalArgumentException.class, () -> {
            imageDAO.saveImage(inputStream, "  ");
        }, "Should throw IllegalArgumentException for blank filename");
    }

    @Test
    void saveImage_shouldSaveSuccessfully_whenFilenameHasNoExtension() throws DAOException {
        InputStream inputStream = getResourceStream("valid.png"); // Use PNG for this test
        String filename = imageDAO.saveImage(inputStream, "testfile_no_extension");

        assertNotNull(filename,
                "Filename should not be null even if original filename has no extension");
        assertFalse(filename.isBlank(), "Filename should not be blank");
        assertTrue(filename.endsWith(".png"),
                "Should be saved with correct extension (.png) based on content");
        assertTrue(Files.exists(tempDir.resolve(IMAGE_SUBFOLDER).resolve(filename)),
                "File should exist in temp dir");
    }

    // --- deleteImage Tests ---

    @Test
    void deleteImage_shouldDeleteExistingFile_whenFileExists() throws DAOException, IOException {
        // Arrange: Save a file first
        InputStream inputStream = getResourceStream("valid.png");
        String filename = imageDAO.saveImage(inputStream, "delete_test.png");

        Path expectedPath = tempDir.resolve(IMAGE_SUBFOLDER).resolve(filename);
        assertTrue(Files.exists(expectedPath), "File should exist after saving");

        // Act & Assert: Delete the file using the filename and assert no exception
        assertDoesNotThrow(() -> imageDAO.deleteImage(filename),
                "Deleting an existing file should not throw");
        assertFalse(Files.exists(expectedPath), "File should not exist after deletion");
    }

    @Test
    void deleteImage_shouldThrowNotFound_whenFileDoesNotExist() {
        // Arrange: A filename that definitely doesn't exist in the temp dir
        String nonExistentFilename = "non_existent_file_" + System.currentTimeMillis() + ".jpg";

        // Act & Assert: Expect DAOException with NOT_FOUND type
        DAOException exception = assertThrows(DAOException.class, () -> {
            imageDAO.deleteImage(nonExistentFilename);
        }, "deleteImage should throw DAOException for a non-existent filename");
        assertEquals(DAOException.DAOErrorType.NOT_FOUND, exception.getErrorType(),
                "Exception type should be NOT_FOUND");
    }

    @Test
    void deleteImage_shouldThrowIllegalArgument_whenFilenameIsNull() {
        assertThrows(IllegalArgumentException.class, () -> {
            imageDAO.deleteImage(null);
        }, "Should throw IllegalArgumentException for null filename");
    }

    @Test
    void deleteImage_shouldThrowIllegalArgument_whenFilenameIsBlank() {
        assertThrows(IllegalArgumentException.class, () -> {
            imageDAO.deleteImage("   ");
        }, "Should throw IllegalArgumentException for blank filename");
    }

    @Test
    void deleteImage_shouldThrowIllegalArgument_whenFilenameIsInvalidFormat() {
        // Filename contains forward slash
        assertThrows(IllegalArgumentException.class, () -> {
            imageDAO.deleteImage("invalid/name.jpg");
        }, "Should throw IllegalArgumentException for filename containing '/'");

        // Filename contains backslash
        assertThrows(IllegalArgumentException.class, () -> {
            imageDAO.deleteImage("invalid\\name.jpg");
        }, "Should throw IllegalArgumentException for filename containing '\\'");

        // Filename is just "."
        assertThrows(IllegalArgumentException.class, () -> {
            imageDAO.deleteImage(".");
        }, "Should throw IllegalArgumentException for filename being '.'");
    }

    @Test
    void deleteImage_shouldThrowIllegalArgument_whenFilenameContainsTraversal() {
        // Attempt to go up directories
        assertThrows(IllegalArgumentException.class, () -> {
            imageDAO.deleteImage("../../../etc/passwd");
        }, "Should throw IllegalArgumentException for filename containing '..'");
    }
}
