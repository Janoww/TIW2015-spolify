package it.polimi.tiw.projects.dao;

import it.polimi.tiw.projects.exceptions.DAOException;
import it.polimi.tiw.projects.exceptions.DAOException.DAOErrorType;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import javax.imageio.ImageIO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// FIXME: To refactor similar to AudioDAO
public class ImageDAO {
    private static final Logger log = LoggerFactory.getLogger(ImageDAO.class);

    private static final String IMAGE_SUBFOLDER = "image";
    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList(".jpg", ".jpeg", ".png", ".webp");
    private static final int MAX_FILENAME_PREFIX_LENGTH = 190;
    private final Path imageStorageDirectory;

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
     * Saves an image from an InputStream to the designated storage directory.
     * Validates the file extension and image content. Generates a unique filename
     * incorporating a sanitized version of the original filename.
     *
     * @param imageStream      The InputStream containing the image data.
     * @param originalFileName The original filename provided by the client (used
     *                         for extension and prefix).
     * @return The relative path (e.g., "image/filename.jpg") of the saved image
     *         within the Spolify directory.
     * @throws DAOException             If an I/O error occurs during file
     *                                  operations (reading stream, creating temp
     *                                  file, moving file).
     * @throws IllegalArgumentException If the file extension is not allowed, the
     *                                  file is not a valid image, or the
     *                                  originalFileName is invalid (e.g., null,
     *                                  empty, no extension).
     */
    public String saveImage(InputStream imageStream, String originalFileName)
            throws DAOException, IllegalArgumentException {
        log.info("Attempting to save image with original filename: {}", originalFileName);
        if (originalFileName == null || originalFileName.isBlank()) {
            log.warn("Save image failed: Original filename was null or blank.");
            throw new IllegalArgumentException("Original filename cannot be null or empty.");
        }

        // 1. Extract extension and validate
        String extension = getFileExtension(originalFileName);
        log.debug("Extracted extension: {}", extension);
        if (extension.isEmpty() || !ALLOWED_EXTENSIONS.contains(extension.toLowerCase())) {
            log.warn("Save image failed: Invalid or unsupported extension '{}' for filename: {}", extension,
                    originalFileName);
            throw new IllegalArgumentException("Invalid or unsupported image file extension: " + extension);
        }

        Path tempFile = null;
        try {
            // 2. Create a temporary file
            tempFile = Files.createTempFile("img_upload_", extension); // Changed prefix for clarity
            log.debug("Created temporary file: {}", tempFile);
            Files.copy(imageStream, tempFile, StandardCopyOption.REPLACE_EXISTING);
            log.debug("Copied input stream to temporary file.");

            // 3. Validate image content
            log.debug("Validating image content...");
            BufferedImage image = ImageIO.read(tempFile.toFile()); // IOException can happen here too
            if (image == null) {
                log.warn("Save image failed: File content validation failed for temp file: {}", tempFile);
                // Clean up before throwing
                deleteTempFile(tempFile);
                throw new IllegalArgumentException(
                        "The uploaded file is not a valid image or the format is corrupted.");
            }
            log.debug("Image content validated successfully.");
            // Optional: Add checks for dimensions, size, etc. if needed
            // log.debug("Image dimensions: {}x{}", image.getWidth(), image.getHeight());
            // log.debug("Temp file size: {} bytes", Files.size(tempFile));

            // 4. Generate final filename
            String finalFilename = generateUniqueFilename(originalFileName, extension);
            Path finalPath = imageStorageDirectory.resolve(finalFilename);
            log.debug("Generated final path: {}", finalPath);

            // 5. Move temporary file to permanent storage
            Files.move(tempFile, finalPath, StandardCopyOption.REPLACE_EXISTING);
            log.info("Successfully saved image to: {}", finalPath);

            // 6. Return relative path
            String relativePath = Paths.get(IMAGE_SUBFOLDER, finalFilename).toString();
            log.debug("Returning relative path: {}", relativePath);
            return relativePath;

        } catch (IOException e) {
            log.error("IOException occurred during image save process for original file {}: {}", originalFileName,
                    e.getMessage(), e);
            // Clean up temp file on error before wrapping and re-throwing
            deleteTempFile(tempFile);
            throw new DAOException("Failed to save image due to I/O error: " + e.getMessage(), e,
                    DAOErrorType.GENERIC_ERROR);
        } catch (IllegalArgumentException e) {
            // Log the validation exception before re-throwing (already logged specific
            // cause)
            log.warn("IllegalArgumentException during image save: {}", e.getMessage());
            // Temp file should have been deleted within the block that threw the exception
            // if validation failed there.
            // If it was thrown earlier (e.g., filename check), tempFile might be null.
            deleteTempFile(tempFile); // Attempt cleanup just in case
            throw e; // Re-throw the original validation exception
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
     * Extracts the file extension (including the dot) from a filename.
     *
     * @param filename The full filename.
     * @return The extension (e.g., ".jpg") or an empty string if no extension is
     *         found.
     */
    private String getFileExtension(String filename) {
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex > 0 && lastDotIndex < filename.length() - 1) {
            return filename.substring(lastDotIndex);
        }
        return "";
    }

    /**
     * Generates a unique filename based on the original filename and a UUID.
     * Sanitizes and truncates the original filename prefix.
     *
     * @param originalFileName The original filename.
     * @param extension        The validated file extension (including the dot).
     * @return A unique filename string.
     */
    private String generateUniqueFilename(String originalFileName, String extension) {
        String baseName = originalFileName.substring(0, originalFileName.length() - extension.length());

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
        return sanitizedBaseName + "_" + uuid + extension;
    }
}
