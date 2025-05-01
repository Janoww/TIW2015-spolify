package it.polimi.tiw.projects.dao;

import it.polimi.tiw.projects.exceptions.DAOException;
import it.polimi.tiw.projects.exceptions.DAOException.DAOErrorType;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Map; // Added Map import
import java.util.Set; // Added Set import (though Map.ofEntries is used)
import java.util.UUID;
// Removed javax.sound imports
import org.apache.tika.Tika; // Added Tika import
// MediaType is no longer needed directly in the validation logic
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AudioDAO {
    private static final Logger log = LoggerFactory.getLogger(AudioDAO.class);

    private static final String BASE_FOLDER_NAME = "Spolify";
    private static final String AUDIO_SUBFOLDER = "song";
    private static final Path STORAGE_DIRECTORY;
    // Map of allowed MIME types (derived from Tika definitions) to their canonical
    // file extensions
    private static final Map<String, String> ALLOWED_MIME_TYPES_MAP = Map.ofEntries(
            // MP3 Types from Tika definition
            Map.entry("audio/mpeg", ".mp3"),
            Map.entry("audio/x-mpeg", ".mp3"), // Alias for audio/mpeg

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

    static {
        // Initialize storage directory path relative to user's home
        String homeDir = System.getProperty("user.home");
        STORAGE_DIRECTORY = Paths.get(homeDir, BASE_FOLDER_NAME, AUDIO_SUBFOLDER);
        try {
            // Create directories if they don't exist
            Files.createDirectories(STORAGE_DIRECTORY);
        } catch (IOException e) {
            log.error("CRITICAL: Could not create audio storage directory: {}", STORAGE_DIRECTORY, e);
            // ? Depending on the application's needs, you might want to throw a
            // ? RuntimeException that prevents the application from starting correctly.
            throw new RuntimeException("Could not initialize audio storage directory", e);
        }
    }

    /**
     * Saves an audio file from an InputStream to the designated storage directory.
     * Validates the file extension. Generates a unique filename
     * incorporating a sanitized version of the original filename.
     *
     * @param audioStream      The InputStream containing the audio data.
     * @param originalFileName The original filename provided by the client (used
     *                         for extension and prefix).
     * @return The relative path (e.g., "song/filename.mp3") of the saved audio file
     *         within the Spolify directory.
     * @throws DAOException             If an I/O error occurs during file
     *                                  operations (reading stream, creating temp
     *                                  file, moving file).
     * @throws IllegalArgumentException If the file extension is not allowed, the
     *                                  originalFileName is invalid (e.g., null,
     *                                  empty, no extension),
     *                                  or if the file content is not a
     *                                  valid/supported audio format.
     */
    public String saveAudio(InputStream audioStream, String originalFileName)
            throws DAOException, IllegalArgumentException {
        log.info("Attempting to save audio with original filename: {}", originalFileName);
        if (originalFileName == null || originalFileName.isBlank()) {
            log.warn("Save audio failed: Original filename was null or blank.");
            throw new IllegalArgumentException("Original filename cannot be null or empty.");
        }

        // Extension from original filename is no longer used for validation or primary
        // type determination.
        // It might be used as a fallback if needed, but the plan is to rely on MIME
        // detection.

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
            String targetExtension = validateAndGetExtension(tempFile); // New validation method
            log.debug("Audio content validated. Target extension: {}", targetExtension);
            // ? Optional: Add further checks for file size if needed
            // ? log.debug("Temp file size: {} bytes", Files.size(tempFile));

            // 3. Generate final filename using the determined extension
            String finalFilename = generateUniqueFilename(originalFileName, targetExtension);
            Path finalPath = STORAGE_DIRECTORY.resolve(finalFilename);
            log.debug("Generated final path: {}", finalPath);

            // 5. Move temporary file to permanent storage
            Files.move(tempFile, finalPath, StandardCopyOption.REPLACE_EXISTING);
            log.info("Successfully saved audio to: {}", finalPath);

            // 6. Return relative path
            String relativePath = Paths.get(AUDIO_SUBFOLDER, finalFilename).toString();
            log.debug("Returning relative path: {}", relativePath);
            return relativePath;

        } catch (IOException e) {
            log.error("IOException occurred during audio save process for original file {}: {}", originalFileName,
                    e.getMessage(), e);
            // Clean up temp file on error before wrapping and re-throwing
            deleteTempFile(tempFile);
            throw new DAOException("Failed to save audio due to I/O error: " + e.getMessage(), e,
                    DAOErrorType.GENERIC_ERROR);
        } catch (IllegalArgumentException e) {
            // Log the validation exception before re-throwing (already logged specific
            // cause)
            log.warn("IllegalArgumentException during audio save: {}", e.getMessage());
            // If it was thrown earlier (e.g., filename check), tempFile might be null.
            deleteTempFile(tempFile); // Attempt cleanup just in case
            throw e; // Re-throw the original validation exception
        }
    }

    /**
     * Validates the audio file content using Apache Tika and determines the
     * appropriate file extension based on the detected MIME type.
     *
     * @param audioFile Path to the temporary audio file to validate.
     * @return The canonical file extension (e.g., ".mp3", ".wav") corresponding to
     *         the
     *         detected and allowed MIME type.
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
            String targetExtension = ALLOWED_MIME_TYPES_MAP.get(mimeType.toLowerCase()); // Use lowercase for lookup
                                                                                         // consistency

            if (targetExtension == null) {
                log.warn("Audio validation failed for {}: Detected audio MIME type '{}' is not supported.", audioFile,
                        mimeType);
                throw new IllegalArgumentException(
                        "The detected audio type (" + mimeType + ") is not supported. Allowed types map to extensions: "
                                + ALLOWED_MIME_TYPES_MAP.values());
            }

            log.debug("Detected MIME type '{}' is supported and maps to extension '{}'.", mimeType, targetExtension);
            return targetExtension;

        } catch (IOException e) {
            log.error("IOException during Tika audio validation for {}: {}", audioFile, e.getMessage(), e);
            // No need to call deleteTempFile here, it's handled in the calling saveAudio's
            // catch block
            throw e; // Re-throw the IOException
        } catch (Exception e) {
            // Catch any other unexpected errors during detection/validation
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

    // getFileExtension method is removed as base name extraction is now part of
    // generateUniqueFilename

    /**
     * Generates a unique filename based on the original filename and a UUID, using
     * the provided target extension.
     * Sanitizes and truncates the original filename prefix.
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
     * Deletes an audio file from the storage directory based on its relative path.
     *
     * @param relativePath The relative path of the file to delete (e.g.,
     *                     "song/filename.mp3").
     * @return true if the file was successfully deleted, false otherwise (e.g.,
     *         file not found).
     * @throws DAOException             If an I/O error occurs during deletion
     *                                  (other than file not found).
     * @throws IllegalArgumentException If the provided relativePath is null, blank,
     *                                  or invalid (e.g., contains '..', refers
     *                                  outside storage).
     */
    public boolean deleteAudio(String relativePath) throws DAOException, IllegalArgumentException {
        log.info("Attempting to delete audio file with relative path: {}", relativePath);

        // 1. Initial checks
        if (relativePath == null || relativePath.isBlank()) {
            log.warn("Delete audio failed: Relative path was null or blank.");
            throw new IllegalArgumentException("Relative path cannot be null or empty for deletion.");
        }

        // Moved Check: Ensure path is not just the subfolder itself (BEFORE try block)
        String subfolderWithSeparator = AUDIO_SUBFOLDER + "/"; // Use forward slash for canonical check
        String subfolderWithBackslash = AUDIO_SUBFOLDER + "\\";
        if (relativePath.equals(subfolderWithSeparator) || relativePath.equals(subfolderWithBackslash)) {
            log.warn("Delete audio failed: Relative path '{}' refers only to the subfolder.", relativePath);
            throw new IllegalArgumentException("Invalid relative path: cannot be just the subfolder name.");
        }

        // 2. Extract filename and perform basic validation
        Path relativePathObj;
        Path fileNamePath;
        try {
            // Basic check for '..' - prevent obvious attempts early
            if (relativePath.contains("..")) {
                log.warn("Delete audio failed: Relative path '{}' contains '..'.", relativePath);
                throw new IllegalArgumentException("Invalid relative path: contains '..'.");
            }

            // Basic check for starting folder - prevent resolving outside structure early
            if (!relativePath.startsWith(AUDIO_SUBFOLDER + "/") && !relativePath.startsWith(AUDIO_SUBFOLDER + "\\")) {
                log.warn("Delete audio failed: Relative path '{}' does not start with the expected subfolder '{}'.",
                        relativePath, AUDIO_SUBFOLDER);
                throw new IllegalArgumentException(
                        "Invalid relative path format: must start with " + AUDIO_SUBFOLDER + "/");
            }

            relativePathObj = Paths.get(relativePath);
            fileNamePath = relativePathObj.getFileName(); // Extract filename part

            // Check if filename part is valid (not null, empty, ., or ..)
            if (fileNamePath == null || fileNamePath.toString().isEmpty() || fileNamePath.toString().equals(".")
                    || fileNamePath.toString().equals("..")) {
                log.warn("Delete audio failed: Invalid filename derived from relative path '{}'.", relativePath);
                throw new IllegalArgumentException("Invalid relative path: does not contain a valid filename.");
            }

        } catch (java.nio.file.InvalidPathException e) {
            log.warn("Delete audio failed: Invalid characters or format in relative path '{}'.", relativePath, e);
            throw new IllegalArgumentException("Invalid relative path format: " + e.getMessage(), e);
        }

        // 3. Construct potential path
        Path potentialPath = STORAGE_DIRECTORY.resolve(fileNamePath.toString());
        log.debug("Constructed potential absolute path: {}", potentialPath);

        try {
            // 4. Validate with toRealPath()
            // Get canonical paths to resolve symlinks and verify existence/containment
            Path storageRealPath = STORAGE_DIRECTORY.toRealPath(); // Ensures storage dir itself is valid
            Path fileRealPath = potentialPath.toRealPath(); // Throws NoSuchFileException if doesn't exist

            log.debug("Storage real path: {}", storageRealPath);
            log.debug("File real path: {}", fileRealPath);

            // Check if the resolved file path is actually inside the resolved storage path
            if (!fileRealPath.startsWith(storageRealPath)) {
                log.warn(
                        "Delete audio failed: Path '{}' resolves outside the designated storage directory '{}'.",
                        fileRealPath, storageRealPath);
                throw new IllegalArgumentException("Invalid relative path: resolves outside storage directory.");
            }

            // 5. Delete the file
            log.debug("Attempting to delete validated file at: {}", fileRealPath);
            // Use deleteIfExists to match previous logic return value (true if deleted,
            // false if not found *at this stage*)
            boolean deleted = Files.deleteIfExists(fileRealPath);
            if (deleted) {
                log.info("Successfully deleted audio file: {}", fileRealPath);
            } else {
                // This case implies a race condition if toRealPath succeeded but deleteIfExists
                // failed.
                log.warn("Audio file not found for deletion after validation or already deleted: {}", fileRealPath);
            }
            return deleted;

        } catch (java.nio.file.NoSuchFileException e) {
            // File doesn't exist at the potential path. This is expected for non-existent
            // files.
            log.warn("Audio file not found for deletion (NoSuchFileException): {}", potentialPath);
            return false; // Consistent with deleteIfExists behavior
        } catch (IOException e) {
            // Includes other I/O errors from toRealPath or deleteIfExists
            log.error("IOException occurred during audio deletion validation or execution for relative path {}: {}",
                    relativePath, e.getMessage(), e);
            throw new DAOException("Failed to delete audio due to I/O error: " + e.getMessage(), e,
                    DAOErrorType.GENERIC_ERROR);
        } catch (SecurityException e) {
            // Security manager prevents access
            log.error("SecurityException occurred during audio deletion for relative path {}: {}", relativePath,
                    e.getMessage(), e);
            throw new DAOException("Failed to delete audio due to security restrictions: " + e.getMessage(), e,
                    DAOErrorType.GENERIC_ERROR);
        } catch (Exception e) { // Catch unexpected errors during validation/deletion
            log.error("Unexpected error during audio deletion for relative path {}: {}", relativePath, e.getMessage(),
                    e);
            throw new DAOException("An unexpected error occurred during audio deletion.", e,
                    DAOErrorType.GENERIC_ERROR);
        }
    }
}
