package it.polimi.tiw.projects.dao;

import it.polimi.tiw.projects.beans.FileData;
import it.polimi.tiw.projects.exceptions.DAOException;
import it.polimi.tiw.projects.exceptions.DAOException.DAOErrorType;
import it.polimi.tiw.projects.utils.StorageUtils;
import org.apache.tika.Tika;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.UUID;

public class AudioDAO {
    private static final Logger log = LoggerFactory.getLogger(AudioDAO.class);

    private static final String AUDIO_SUBFOLDER = "song";
    // Map of allowed MIME types (derived from Tika definitions) to their canonical
    // file extensions
    private static final Map<String, String> ALLOWED_MIME_TYPES_MAP = Map.ofEntries(
            // MP3 Types from Tika definition
            Map.entry("audio/mpeg", ".mp3"), Map.entry("audio/x-mpeg", ".mp3"), // Alias for
            // audio/mpeg

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
    private final Path songStorageDirectory;

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
     * file within the 'song' directory.
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
            // Create a temporary file (use a generic suffix initially)
            tempFile = Files.createTempFile("audio_upload_", ".tmp");
            log.debug("Created temporary file: {}", tempFile);
            Files.copy(audioStream, tempFile, StandardCopyOption.REPLACE_EXISTING);
            log.debug("Copied input stream to temporary file.");

            // Validate content and get the correct extension based on MIME type
            log.debug("Validating audio file content and determining extension...");
            String targetExtension = validateAndGetExtension(tempFile);
            log.debug("Audio content validated. Target extension: {}", targetExtension);

            // Generate final filename using the determined extension
            String finalFilename = generateUniqueFilename(originalFileName, targetExtension);
            Path finalPath = this.songStorageDirectory.resolve(finalFilename);
            log.debug("Generated final path: {}", finalPath);

            // Move temporary file to permanent storage
            Files.move(tempFile, finalPath, StandardCopyOption.REPLACE_EXISTING);
            log.info("Successfully saved audio to: {}", finalPath);

            // Return the final filename (relative to the song storage directory)
            log.debug("Returning final filename: {}", finalFilename);
            return finalFilename;

        } catch (IOException e) {
            log.error("IOException occurred during audio save process for original file {}: {}", originalFileName,
                    e.getMessage(), e);
            deleteTempFile(tempFile);
            throw new DAOException("Failed to save audio due to I/O error: " + e.getMessage(), e,
                    DAOErrorType.GENERIC_ERROR);
        } catch (IllegalArgumentException e) {
            log.warn("IllegalArgumentException during audio save: {}", e.getMessage());
            deleteTempFile(tempFile);
            throw e;
        }
    }

    /**
     * Validates the audio file content using Apache Tika and determines the
     * appropriate file extension based on the detected MIME type.
     *
     * @param audioFile Path to the temporary audio file to validate.
     * @return The canonical file extension (e.g., ".mp3", ".wav") corresponding to
     * the detected and allowed MIME type.
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

            // Basic check
            if (mimeType == null || !mimeType.startsWith("audio/")) {
                log.warn("Audio validation failed for {}: Detected MIME type '{}' is not audio.", audioFile, mimeType);
                throw new IllegalArgumentException(
                        "The uploaded file is not recognized as a valid audio format (detected type: " + mimeType
                                + ").");
            }

            // Specific check for allowed mimetypes
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
            log.error("Unexpected error during audio validation for {}: {}", audioFile, e.getMessage(), e);
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

        // Validate filename for early exit
        if (filename == null || filename.isBlank()) {
            log.warn("Delete audio failed: Filename was null or blank.");
            throw new IllegalArgumentException("Filename cannot be null or empty for deletion.");
        }
        // Specific check for problematic names
        if (filename.startsWith(".")) {
            log.warn("Delete audio failed: Filename '{}' is invalid for deletion.", filename);
            throw new IllegalArgumentException("Invalid filename for deletion.");
        }

        try {
            // Validate, resolve, and check existence using utility
            Path fileRealPath = StorageUtils.validateAndResolveSecurePath(filename, this.songStorageDirectory);
            log.debug("Path validated for deletion: {}", fileRealPath);

            // Delete the file
            boolean deleted = Files.deleteIfExists(fileRealPath);
            if (deleted) {
                log.info("Successfully deleted audio file: {}", fileRealPath);
            } else {
                log.warn("Audio file not found for deletion (deleteIfExists returned false after validation): {}",
                        fileRealPath);
                throw new DAOException("Audio file disappeared before deletion: " + filename, DAOErrorType.NOT_FOUND);
            }
        } catch (DAOException | IllegalArgumentException e) {
            log.warn("Validation or access error during audio deletion for {}: {}", filename, e.getMessage());
            throw e;
        } catch (IOException e) {
            log.error("IOException occurred during audio file deletion for {}: {}", filename, e.getMessage(), e);
            throw new DAOException("Failed to delete audio due to I/O error: " + e.getMessage(), e,
                    DAOErrorType.GENERIC_ERROR);
        } catch (SecurityException e) {
            log.error("SecurityException occurred during audio file deletion for {}: {}", filename, e.getMessage(), e);
            throw new DAOException("Failed to delete audio due to security restrictions: " + e.getMessage(), e,
                    DAOErrorType.GENERIC_ERROR);
        } catch (Exception e) {
            log.error("Unexpected error during audio deletion for filename {}: {}", filename, e.getMessage(), e);
            throw new DAOException("An unexpected error occurred during audio deletion.", e,
                    DAOErrorType.GENERIC_ERROR);
        }
    }

    /**
     * Retrieves an audio file's data and metadata.
     *
     * @param filename The unique filename of the audio (e.g., "filename_uuid.mp3")
     *                 stored in the song directory.
     * @return A FileData object containing the audio's content stream and metadata.
     * @throws DAOException             If the file is not found, cannot be
     *                                  accessed, or an I/O error occurs.
     * @throws IllegalArgumentException If the filename is invalid (null, blank, or
     *                                  contains path traversal).
     */
    public FileData getAudio(String filename) throws DAOException, IllegalArgumentException {
        log.info("Attempting to retrieve audio file with filename: {}", filename);

        try {
            // Validate, resolve, and check existence using utility
            Path fileRealPath = StorageUtils.validateAndResolveSecurePath(filename, this.songStorageDirectory);
            log.debug("Path validated for retrieval: {}", fileRealPath);

            // Get metadata and open stream
            String mimeType = new Tika().detect(fileRealPath);
            long size = Files.size(fileRealPath);
            InputStream contentStream = Files.newInputStream(fileRealPath);

            log.info("Successfully prepared FileData for audio: {}", filename);
            return new FileData(contentStream, filename, mimeType, size);

        } catch (DAOException | IllegalArgumentException e) {
            log.warn("Validation or access error during audio retrieval for {}: {}", filename, e.getMessage());
            throw e;
        } catch (IOException e) {
            log.error("IOException occurred during audio metadata/content retrieval for {}: {}", filename,
                    e.getMessage(), e);
            throw new DAOException("Failed to retrieve audio metadata or content due to I/O error: " + e.getMessage(),
                    e, DAOErrorType.GENERIC_ERROR);
        } catch (SecurityException e) {
            log.error("SecurityException occurred during audio metadata/content retrieval for {}: {}", filename,
                    e.getMessage(), e);
            throw new DAOException(
                    "Failed to retrieve audio metadata or content due to security restrictions: " + e.getMessage(), e,
                    DAOErrorType.GENERIC_ERROR);
        }
    }
}
