package it.polimi.tiw.projects.dao;

import it.polimi.tiw.projects.exceptions.DAOException;
import it.polimi.tiw.projects.exceptions.DAOException.DAOErrorType;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.UUID;
import org.apache.tika.Tika;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ImageDAO {
    private static final Logger log = LoggerFactory.getLogger(ImageDAO.class);

    private static final String IMAGE_SUBFOLDER = "image";
    private final Path imageStorageDirectory;

    // Map of allowed MIME types to their canonical file extensions
    // Tika detects both .jpg and .jpeg as image/jpeg
    private static final Map<String, String> ALLOWED_MIME_TYPES_MAP = Map.ofEntries(
            Map.entry("image/jpeg", ".jpg"), // Use .jpg for image/jpeg
            Map.entry("image/png", ".png"),
            Map.entry("image/webp", ".webp"));
    private static final int MAX_FILENAME_PREFIX_LENGTH = 190;

    /**
     * Constructs an ImageDAO with a specified base storage directory. The 'image'
     * subdirectory within this base directory will be created if it doesn't exist.
     *
     * @param baseStorageDirectory The Path object representing the base directory
     *                             (e.g., where 'image' subfolder should reside).
     * @throws RuntimeException if the 'image' subdirectory cannot be created within
     *                          the base directory.
     */
    public ImageDAO(Path baseStorageDirectory) {
        this.imageStorageDirectory = baseStorageDirectory.resolve(IMAGE_SUBFOLDER).normalize();

        try {
            // Create the specific song subdirectory if it doesn't exist
            Files.createDirectories(this.imageStorageDirectory);
            log.info("ImageDAO initialized. Image storage directory: {}", this.imageStorageDirectory);
        } catch (IOException e) {
            log.error("CRITICAL: Could not create image storage directory: {}", this.imageStorageDirectory, e);
            // Throw a runtime exception as the image DAO cannot function without its
            // storage
            throw new RuntimeException("Could not initialize image storage directory", e);
        } catch (SecurityException e) {
            log.error("CRITICAL: Security permissions prevent creating image storage directory: {}",
                    this.imageStorageDirectory, e);
            throw new RuntimeException("Security permissions prevent creating image storage directory", e);
        }
    }

    /**
     * Saves an image file from an InputStream to the configured 'image' storage
     * directory. Validates the file content type using Tika. Generates a unique
     * filename incorporating a sanitized version of the original filename.
     *
     * @param imageStream      The InputStream containing the image data.
     * @param originalFileName The original filename provided by the client (used
     *                         for prefix).
     * @return The unique filename (e.g., "filename_uuid.jpg") of the saved image
     *         file within the 'image' directory.
     * @throws DAOException             If an I/O error occurs during file
     *                                  operations.
     * @throws IllegalArgumentException If the file content is not a valid/supported
     *                                  image format, or if the originalFileName is
     *                                  invalid.
     */
    public String saveImage(InputStream imageStream, String originalFileName)
            throws DAOException, IllegalArgumentException {
        log.info("Attempting to save image with original filename: {}", originalFileName);
        if (originalFileName == null || originalFileName.isBlank()) {
            log.warn("Save image failed: Original filename was null or blank.");
            throw new IllegalArgumentException("Original filename cannot be null or empty.");
        }

        Path tempFile = null;
        try {
            // 1. Create a temporary file (use a generic suffix initially)
            tempFile = Files.createTempFile("img_upload_", ".tmp");
            log.debug("Created temporary file: {}", tempFile);
            Files.copy(imageStream, tempFile, StandardCopyOption.REPLACE_EXISTING);
            log.debug("Copied input stream to temporary file.");

            // 2. Validate content and get the correct extension based on MIME type
            log.debug("Validating image file content and determining extension...");
            String targetExtension = validateAndGetExtension(tempFile);
            log.debug("Image content validated. Target extension: {}", targetExtension);

            // 3. Generate final filename using the determined extension
            String finalFilename = generateUniqueFilename(originalFileName, targetExtension);
            Path finalPath = this.imageStorageDirectory.resolve(finalFilename);
            log.debug("Generated final path: {}", finalPath);

            // 4. Move temporary file to permanent storage
            Files.move(tempFile, finalPath, StandardCopyOption.REPLACE_EXISTING);
            log.info("Successfully saved image to: {}", finalPath);

            // 5. Return the final filename (relative to the image storage directory)
            log.debug("Returning final filename: {}", finalFilename);
            return finalFilename; // Return only the filename, not the relative path

        } catch (IOException e) {
            log.error("IOException occurred during image save process for original file {}: {}", originalFileName,
                    e.getMessage(), e);
            // Clean up temp file on error before wrapping and re-throwing
            deleteTempFile(tempFile);
            throw new DAOException("Failed to save image due to I/O error: " + e.getMessage(), e,
                    DAOErrorType.GENERIC_ERROR);
        } catch (IllegalArgumentException e) {
            log.warn("IllegalArgumentException during image save: {}", e.getMessage());
            // If it was thrown earlier (e.g., filename check), tempFile might be null.
            deleteTempFile(tempFile); // Attempt cleanup just in case
            throw e;
        }
    }

    /**
     * Validates the image file content using Apache Tika and determines the
     * appropriate file extension based on the detected MIME type.
     *
     * @param imageFile Path to the temporary image file to validate.
     * @return The canonical file extension (e.g., ".jpg", ".png") corresponding to
     *         the detected and allowed MIME type.
     * @throws IllegalArgumentException if the file is not detected as a supported
     *                                  image format based on the
     *                                  ALLOWED_MIME_TYPES_MAP.
     * @throws IOException              if an I/O error occurs reading the file
     *                                  during detection.
     */
    private String validateAndGetExtension(Path imageFile) throws IllegalArgumentException, IOException {
        Tika tika = new Tika();
        String mimeType = null;
        try {
            mimeType = tika.detect(imageFile);
            log.debug("Detected MIME type for {}: {}", imageFile, mimeType);

            // Basic check: Is it an image?
            if (mimeType == null || !mimeType.startsWith("image/")) {
                log.warn("Image validation failed for {}: Detected MIME type '{}' is not image.", imageFile, mimeType);
                throw new IllegalArgumentException(
                        "The uploaded file is not recognized as a valid image format (detected type: " + mimeType
                                + ").");
            }

            // Specific check: Is the detected MIME type in our allowed map?
            String targetExtension = ALLOWED_MIME_TYPES_MAP.get(mimeType.toLowerCase());
            if (targetExtension == null) {
                log.warn("Image validation failed for {}: Detected image MIME type '{}' is not supported.", imageFile,
                        mimeType);
                throw new IllegalArgumentException("The detected image type (" + mimeType
                        + ") is not supported. Allowed types map to extensions: " + ALLOWED_MIME_TYPES_MAP.values());
            }

            log.debug("Detected MIME type '{}' is supported and maps to extension '{}'.", mimeType, targetExtension);
            return targetExtension;

        } catch (IOException e) {
            log.error("IOException during Tika image validation for {}: {}", imageFile, e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            // Catch any other unexpected errors during detection/validation
            log.error("Unexpected error during image validation for {}: {}", imageFile, e.getMessage(), e);
            throw new IllegalArgumentException("An unexpected error occurred during image file validation.", e);
        }
    }

    /**
     * Helper method to delete a temporary file, logging any secondary errors.
     * 
     * @param tempFile The path to the temporary file (can be null).
     */
    private void deleteTempFile(Path tempFile) {
        if (tempFile != null) {
            try {
                Files.deleteIfExists(tempFile);
                log.debug("Deleted temporary file: {}", tempFile);
            } catch (IOException suppress) {
                log.error("Failed to delete temporary file {} during cleanup: {}", tempFile, suppress.getMessage(),
                        suppress);
            }
        }
    }

    /**
     * Generates a unique filename based on the original filename and a UUID, using
     * the provided target extension. Sanitizes and truncates the original filename
     * prefix.
     *
     * @param originalFileName The original filename (used to derive the base name).
     * @param targetExtension  The validated file extension determined by MIME type
     *                         (including the dot).
     * @return A unique filename string.
     */
    private String generateUniqueFilename(String originalFileName, String targetExtension) {
        String baseName;
        int lastDotIndex = originalFileName.lastIndexOf('.');
        // Extract base name robustly, handling cases with and without extensions
        if (lastDotIndex > 0) {
            baseName = originalFileName.substring(0, lastDotIndex);
        } else {
            baseName = originalFileName; // Use the whole name if no dot found
        }

        // Sanitize: Replace non-alphanumeric characters (except underscore/hyphen) with
        // underscore
        String sanitizedBaseName = baseName.replaceAll("[^a-zA-Z0-9_\\-]", "_");

        // Truncate if necessary
        if (sanitizedBaseName.length() > MAX_FILENAME_PREFIX_LENGTH) {
            sanitizedBaseName = sanitizedBaseName.substring(0, MAX_FILENAME_PREFIX_LENGTH);
        }
        // Ensure it doesn't end with an underscore if truncated or sanitized that way
        sanitizedBaseName = sanitizedBaseName.replaceAll("_+$", "");
        if (sanitizedBaseName.isEmpty()) {
            sanitizedBaseName = "image"; // Default if sanitization results in empty string
        }

        String uuid = UUID.randomUUID().toString();
        // Use the targetExtension determined from MIME type
        return sanitizedBaseName + "_" + uuid + targetExtension;
    }

    /**
     * Deletes an image file from the configured 'image' storage directory based on
     * its filename.
     *
     * @param filename The filename of the file to delete (e.g.,
     *                 "filename_uuid.jpg"). Must not contain path separators.
     * @throws DAOException             If the file is not found after validation,
     *                                  or if an I/O error occurs during deletion.
     * @throws IllegalArgumentException If the provided filename is null, blank,
     *                                  contains path separators ('/' or '\'), or
     *                                  attempts path traversal ('..').
     */
    public void deleteImage(String filename) throws DAOException, IllegalArgumentException {
        log.info("Attempting to delete image file with filename: {}", filename);

        // 1. Validate filename
        if (filename == null || filename.isBlank()) {
            log.warn("Delete image failed: Filename was null or blank.");
            throw new IllegalArgumentException("Filename cannot be null or empty for deletion.");
        }
        // Check for path separators or traversal attempts
        if (filename.contains("/") || filename.contains("\\") || filename.contains("..")) {
            log.warn("Delete image failed: Filename '{}' contains invalid characters (path separators or '..').",
                    filename);
            throw new IllegalArgumentException(
                    "Invalid filename: must not contain path separators ('/', '\\') or '..'.");
        }
        // Check for problematic names like "." or just an extension
        if (filename.equals(".") || filename.startsWith(".")) {
            log.warn("Delete image failed: Filename '{}' is invalid.", filename);
            throw new IllegalArgumentException("Invalid filename provided.");
        }

        // 2. Construct potential path within the configured image storage directory
        Path potentialPath;
        try {
            potentialPath = this.imageStorageDirectory.resolve(filename).normalize();
            log.debug("Constructed potential absolute path: {}", potentialPath);
        } catch (InvalidPathException e) {
            log.warn("Delete image failed: Invalid path generated from filename '{}'.", filename, e);
            throw new IllegalArgumentException("Invalid filename resulting in invalid path: " + e.getMessage(), e);
        }

        try {
            // 3. Validate that the resolved path is still within the image storage
            // directory
            Path imageStorageRealPath = this.imageStorageDirectory.toRealPath();
            Path fileRealPath = potentialPath.toRealPath();

            log.debug("Image storage real path: {}", imageStorageRealPath);
            log.debug("File real path: {}", fileRealPath);

            if (!fileRealPath.startsWith(imageStorageRealPath)) {
                log.warn(
                        "Delete image failed: Filename '{}' resolves to path '{}' which is outside the designated image storage directory '{}'.",
                        filename, fileRealPath, imageStorageRealPath);
                throw new IllegalArgumentException("Invalid filename: resolves outside storage directory.");
            }

            // 4. Delete the file
            log.debug("Attempting to delete validated file at: {}", fileRealPath);

            boolean deleted = Files.deleteIfExists(fileRealPath);
            if (deleted) {
                log.info("Successfully deleted image file: {}", fileRealPath);
            } else {
                log.warn("Image file not found for deletion (deleteIfExists returned false): {}", fileRealPath);
                throw new DAOException("Image file not found for deletion: " + filename, DAOErrorType.NOT_FOUND);
            }

        } catch (NoSuchFileException e) {
            log.warn("Image file not found for deletion (NoSuchFileException during path validation): {}",
                    potentialPath);
            throw new DAOException("Image file not found for deletion: " + filename, DAOErrorType.NOT_FOUND);
        } catch (IOException e) {
            log.error("IOException occurred during image deletion for relative path {}: {}", filename, e.getMessage(),
                    e);
            throw new DAOException("Failed to delete image due to I/O error: " + e.getMessage(), e,
                    DAOErrorType.GENERIC_ERROR);
        } catch (SecurityException e) {
            log.error("SecurityException occurred during image deletion for filename {}: {}", filename, e.getMessage(),
                    e);
            throw new DAOException("Failed to delete image due to security restrictions: " + e.getMessage(), e,
                    DAOErrorType.GENERIC_ERROR);
        } catch (Exception e) { // Catch unexpected errors
            log.error("Unexpected error during image deletion for filename {}: {}", filename, e.getMessage(), e);
            throw new DAOException("An unexpected error occurred during image deletion.", e,
                    DAOErrorType.GENERIC_ERROR);
        }
    }
}
