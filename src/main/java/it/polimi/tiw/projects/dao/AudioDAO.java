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

public class AudioDAO {
    private static final Logger log = LoggerFactory.getLogger(AudioDAO.class);

    private static final String AUDIO_SUBFOLDER = "song";
    private final Path songStorageDirectory;

    // Map of allowed MIME types (derived from Tika definitions) to their canonical
    // file extensions
    private static final Map<String, String> ALLOWED_MIME_TYPES_MAP = Map.ofEntries(
            // MP3 Types from Tika definition
            Map.entry("audio/mpeg", ".mp3"), Map.entry("audio/x-mpeg", ".mp3"), // Alias for audio/mpeg

            // WAV Types from Tika definition
            Map.entry("audio/vnd.wave", ".wav"), // Primary WAV type
            Map.entry("audio/wav", ".wav"), // Alias
            Map.entry("audio/wave", ".wav"), // Alias
            Map.entry("audio/x-wav", ".wav"), // Alias

            // OGG Types from Tika definition relevant to .ogg extension
            Map.entry("audio/ogg", ".ogg"), // General Ogg audio
            Map.entry("audio/vorbis", ".ogg") // Specific Vorbis codec in Ogg
    );
    private static final int MAX_FILENAME_PREFIX_LENGTH = 190;

    /**
     * Constructs an AudioDAO with a specified base storage directory. The 'song'
     * subdirectory within this base directory will be created if it doesn't exist.
     *
     * @param baseStorageDirectory The Path object representing the base directory
     *                             (e.g., where 'song' subfolder should reside).
     * @throws RuntimeException if the 'song' subdirectory cannot be created within
     *                          the base directory.
     */
    public AudioDAO(Path baseStorageDirectory) {
        this.songStorageDirectory = baseStorageDirectory.resolve(AUDIO_SUBFOLDER).normalize();

        try {
            // Create the specific song subdirectory if it doesn't exist
            Files.createDirectories(this.songStorageDirectory);
            log.info("AudioDAO initialized. Song storage directory: {}", this.songStorageDirectory);
        } catch (IOException e) {
            log.error("CRITICAL: Could not create audio storage directory: {}", this.songStorageDirectory, e);
            // Throw a runtime exception as the DAO cannot function without its storage
            throw new RuntimeException("Could not initialize audio storage directory", e);
        } catch (SecurityException e) {
            log.error("CRITICAL: Security permissions prevent creating audio storage directory: {}",
                    this.songStorageDirectory, e);
            throw new RuntimeException("Security permissions prevent creating audio storage directory", e);
        }
    }

    /**
     * Saves an audio file from an InputStream to the configured 'song' storage
     * directory. Validates the file content type. Generates a unique filename
     * incorporating a sanitized version of the original filename.
     *
     * @param audioStream      The InputStream containing the audio data.
     * @param originalFileName The original filename provided by the client (used
     *                         for prefix).
     * @return The unique filename (e.g., "filename_uuid.mp3") of the saved audio
     *         file within the 'song' directory.
     * @throws DAOException             If an I/O error occurs during file
     *                                  operations.
     * @throws IllegalArgumentException If the file content is not a valid/supported
     *                                  audio format, or if the originalFileName is
     *                                  invalid.
     */
    public String saveAudio(InputStream audioStream, String originalFileName)
            throws DAOException, IllegalArgumentException {
        log.info("Attempting to save audio with original filename: {}", originalFileName);
        if (originalFileName == null || originalFileName.isBlank()) {
            log.warn("Save audio failed: Original filename was null or blank.");
            throw new IllegalArgumentException("Original filename cannot be null or empty.");
        }

        Path tempFile = null;
        try {
            // 1. Create a temporary file (use a generic suffix initially)
            // We don't know the correct extension yet.
            tempFile = Files.createTempFile("audio_upload_", ".tmp");
            log.debug("Created temporary file: {}", tempFile);
            Files.copy(audioStream, tempFile, StandardCopyOption.REPLACE_EXISTING);
            log.debug("Copied input stream to temporary file.");

            // 2. Validate content and get the correct extension based on MIME type
            log.debug("Validating audio file content and determining extension...");
            String targetExtension = validateAndGetExtension(tempFile);
            log.debug("Audio content validated. Target extension: {}", targetExtension);

            // 3. Generate final filename using the determined extension
            String finalFilename = generateUniqueFilename(originalFileName, targetExtension);
            Path finalPath = this.songStorageDirectory.resolve(finalFilename);
            log.debug("Generated final path: {}", finalPath);

            // 4. Move temporary file to permanent storage
            Files.move(tempFile, finalPath, StandardCopyOption.REPLACE_EXISTING);
            log.info("Successfully saved audio to: {}", finalPath);

            // 5. Return the final filename (relative to the song storage directory)
            log.debug("Returning final filename: {}", finalFilename);
            return finalFilename;

        } catch (IOException e) {
            log.error("IOException occurred during audio save process for original file {}: {}", originalFileName,
                    e.getMessage(), e);
            // Clean up temp file on error before wrapping and re-throwing
            deleteTempFile(tempFile);
            throw new DAOException("Failed to save audio due to I/O error: " + e.getMessage(), e,
                    DAOErrorType.GENERIC_ERROR);
        } catch (IllegalArgumentException e) {
            log.warn("IllegalArgumentException during audio save: {}", e.getMessage());
            // If it was thrown earlier (e.g., filename check), tempFile might be null.
            deleteTempFile(tempFile); // Attempt cleanup just in case
            throw e;
        }
    }

    /**
     * Validates the audio file content using Apache Tika and determines the
     * appropriate file extension based on the detected MIME type.
     *
     * @param audioFile Path to the temporary audio file to validate.
     * @return The canonical file extension (e.g., ".mp3", ".wav") corresponding to
     *         the detected and allowed MIME type.
     * @throws IllegalArgumentException if the file is not detected as a supported
     *                                  audio format based on the
     *                                  ALLOWED_MIME_TYPES_MAP.
     * @throws IOException              if an I/O error occurs reading the file
     *                                  during detection.
     */
    private String validateAndGetExtension(Path audioFile) throws IllegalArgumentException, IOException {
        Tika tika = new Tika();
        String mimeType = null;
        try {
            mimeType = tika.detect(audioFile);
            log.debug("Detected MIME type for {}: {}", audioFile, mimeType);

            // Basic check: Is it audio?
            if (mimeType == null || !mimeType.startsWith("audio/")) {
                log.warn("Audio validation failed for {}: Detected MIME type '{}' is not audio.", audioFile, mimeType);
                throw new IllegalArgumentException(
                        "The uploaded file is not recognized as a valid audio format (detected type: " + mimeType
                                + ").");
            }

            // Specific check: Is the detected MIME type in our allowed map?
            String targetExtension = ALLOWED_MIME_TYPES_MAP.get(mimeType.toLowerCase());
            if (targetExtension == null) {
                log.warn("Audio validation failed for {}: Detected audio MIME type '{}' is not supported.", audioFile,
                        mimeType);
                throw new IllegalArgumentException("The detected audio type (" + mimeType
                        + ") is not supported. Allowed types map to extensions: " + ALLOWED_MIME_TYPES_MAP.values());
            }

            log.debug("Detected MIME type '{}' is supported and maps to extension '{}'.", mimeType, targetExtension);
            return targetExtension;

        } catch (IOException e) {
            log.error("IOException during Tika audio validation for {}: {}", audioFile, e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            // Catch any other unexpected errors during detection/validation
            log.error(
                    "Unexpected error during audio validation for {}: {}. This is being wrapped as IllegalArgumentException.",
                    audioFile, e.getMessage(), e);
            throw new IllegalArgumentException("An unexpected error occurred during audio file validation.", e);
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
            sanitizedBaseName = "audio"; // Default if sanitization results in empty string
        }

        String uuid = UUID.randomUUID().toString();
        // Use the targetExtension determined from MIME type
        return sanitizedBaseName + "_" + uuid + targetExtension;
    }

    /**
     * Deletes an audio file from the configured 'song' storage directory based on
     * its filename.
     *
     * @param filename The filename of the file to delete (e.g.,
     *                 "filename_uuid.mp3"). Must not contain path separators.
     * @throws DAOException             If the file is not found after validation,
     *                                  or if an I/O error occurs during deletion.
     * @throws IllegalArgumentException If the provided filename is null, blank,
     *                                  contains path separators ('/' or '\'), or
     *                                  attempts path traversal ('..').
     */
    public void deleteAudio(String filename) throws DAOException, IllegalArgumentException {
        log.info("Attempting to delete audio file with filename: {}", filename);

        // 1. Validate filename
        if (filename == null || filename.isBlank()) {
            log.warn("Delete audio failed: Filename was null or blank.");
            throw new IllegalArgumentException("Filename cannot be null or empty for deletion.");
        }
        // Check for path separators or traversal attempts
        if (filename.contains("/") || filename.contains("\\") || filename.contains("..")) {
            log.warn("Delete audio failed: Filename '{}' contains invalid characters (path separators or '..').",
                    filename);
            throw new IllegalArgumentException(
                    "Invalid filename: must not contain path separators ('/', '\\') or '..'.");
        }
        // Check for problematic names like "." or just an extension (though unlikely
        // given generation logic)
        if (filename.equals(".") || filename.startsWith(".")) {
            log.warn("Delete audio failed: Filename '{}' is invalid.", filename);
            throw new IllegalArgumentException("Invalid filename provided.");
        }

        // 2. Construct potential path within the configured song storage directory
        Path potentialPath;
        try {
            // Resolve filename against the specific song storage directory
            potentialPath = this.songStorageDirectory.resolve(filename).normalize();
            log.debug("Constructed potential absolute path: {}", potentialPath);
        } catch (InvalidPathException e) {
            // Should be rare if filename validation above is robust, but catch just in case
            log.warn("Delete audio failed: Invalid path generated from filename '{}'.", filename, e);
            throw new IllegalArgumentException("Invalid filename resulting in invalid path: " + e.getMessage(), e);
        }

        try {
            // 3. Validate that the resolved path is still within the song storage directory
            Path songStorageRealPath = this.songStorageDirectory.toRealPath();
            Path fileRealPath = potentialPath.toRealPath();

            log.debug("Song storage real path: {}", songStorageRealPath);
            log.debug("File real path: {}", fileRealPath);

            // Check if the resolved file path is actually inside the resolved song storage
            // path
            if (!fileRealPath.startsWith(songStorageRealPath)) {
                log.warn(
                        "Delete audio failed: Filename '{}' resolves to path '{}' which is outside the designated song storage directory '{}'.",
                        filename, fileRealPath, songStorageRealPath);
                // This indicates a potential security issue or logic error
                throw new IllegalArgumentException("Invalid filename: resolves outside storage directory.");
            }

            // 4. Delete the file
            log.debug("Attempting to delete validated file at: {}", fileRealPath);

            boolean deleted = Files.deleteIfExists(fileRealPath); // Can throw IOException, SecurityException
            if (deleted) {
                log.info("Successfully deleted audio file: {}", fileRealPath);
            } else {
                // File existed during toRealPath but was gone by deleteIfExists, or
                // deleteIfExists failed silently?
                // Treat as not found for consistency.
                log.warn("Audio file not found for deletion (deleteIfExists returned false): {}", fileRealPath);
                throw new DAOException("Audio file not found for deletion: " + filename, DAOErrorType.NOT_FOUND);
            }

        } catch (NoSuchFileException e) {
            // Catch if toRealPath couldn't find the file initially
            log.warn("Audio file not found for deletion (NoSuchFileException during path validation): {}",
                    potentialPath);
            throw new DAOException("Audio file not found for deletion: " + filename, DAOErrorType.NOT_FOUND);
        } catch (IOException e) {
            // Catch other I/O errors (permissions, etc.) from toRealPath or deleteIfExists
            log.error("IOException occurred during audio deletion for relative path {}: {}", filename, e.getMessage(),
                    e);
            throw new DAOException("Failed to delete audio due to I/O error: " + e.getMessage(), e,
                    DAOErrorType.GENERIC_ERROR);
        } catch (SecurityException e) {
            // Security manager prevents access
            log.error("SecurityException occurred during audio deletion for filename {}: {}", filename, e.getMessage(),
                    e);
            throw new DAOException("Failed to delete audio due to security restrictions: " + e.getMessage(), e,
                    DAOErrorType.GENERIC_ERROR);
        } catch (Exception e) { // Catch unexpected errors during validation/deletion
            log.error("Unexpected error during audio deletion for filename {}: {}", filename, e.getMessage(), e);
            throw new DAOException("An unexpected error occurred during audio deletion.", e,
                    DAOErrorType.GENERIC_ERROR);
        }
    }
}
