package it.polimi.tiw.projects.utils;

import it.polimi.tiw.projects.exceptions.DAOException;
import it.polimi.tiw.projects.exceptions.DAOException.DAOErrorType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.InvalidPathException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;

public class StorageUtils {

    private static final Logger log = LoggerFactory.getLogger(StorageUtils.class);

    // Private constructor to prevent instantiation
    private StorageUtils() {
    }

    /**
     * Validates a filename, resolves it against a base storage directory, and
     * ensures the resulting path is securely within that directory. Also verifies
     * file existence.
     *
     * @param filename             The filename to validate and resolve.
     * @param baseStorageDirectory The base directory against which the filename is
     *                             resolved.
     * @return The validated, normalized, and real (existent) Path object.
     * @throws DAOException             If the file is not found, or if the resolved
     *                                  path is outside the base storage directory
     *                                  (indicating an access attempt violation).
     * @throws IllegalArgumentException If the filename is null, blank, contains
     *                                  invalid path characters, or results in an
     *                                  invalid path.
     */
    public static Path validateAndResolveSecurePath(String filename, Path baseStorageDirectory)
            throws DAOException, IllegalArgumentException {

        log.debug("Validating and resolving secure path for filename: {} in base directory: {}", filename,
                baseStorageDirectory);

        // Validate filename syntax
        if (filename == null || filename.isBlank()) {
            log.warn("Validation failed: Filename was null or blank.");
            throw new IllegalArgumentException("Filename cannot be null or empty.");
        }
        if (filename.contains("/") || filename.contains("\\") || filename.contains("..")) {
            log.warn("Validation failed: Filename '{}' contains invalid path characters.", filename);
            throw new IllegalArgumentException(
                    "Invalid filename: must not contain path separators ('/', '\\') or '..'.");
        }

        // Construct potential path
        Path potentialFilePath;
        try {
            potentialFilePath = baseStorageDirectory.resolve(filename).normalize();
            log.debug("Constructed potential path: {}", potentialFilePath);
        } catch (InvalidPathException e) {
            log.warn("Validation failed: Invalid path generated from filename '{}'.", filename, e);
            throw new IllegalArgumentException(
                    "Invalid filename resulting in invalid path: " + e.getMessage(), e);
        }

        // Security check:
        // Ensure the resolved path is within the storage directory and file exists
        try {
            Path baseStorageRealPath = baseStorageDirectory.toRealPath();
            Path fileRealPath = potentialFilePath.toRealPath();

            log.debug("Base storage real path: {}", baseStorageRealPath);
            log.debug("File real path for validation: {}", fileRealPath);

            if (!fileRealPath.startsWith(baseStorageRealPath)) {
                log.warn("Security validation failed: Filename '{}' resolves to path '{}' which is outside the designated storage directory '{}'.",
                        filename, fileRealPath, baseStorageRealPath);
                throw new DAOException("Access denied: Filename resolves outside storage directory.",
                        DAOErrorType.ACCESS_DENIED);
            }

            log.debug("Path validation successful for: {}", fileRealPath);
            return fileRealPath;

        } catch (NoSuchFileException e) {
            log.warn("Validation failed: File not found at path '{}' derived from filename '{}'.",
                    potentialFilePath, filename, e);
            throw new DAOException("File not found: " + filename, e, DAOErrorType.NOT_FOUND);
        } catch (IOException e) {
            log.error("IOException during path validation for filename '{}' in {}: {}", filename,
                    baseStorageDirectory, e.getMessage(), e);
            throw new DAOException("Failed to validate path due to I/O error: " + e.getMessage(), e,
                    DAOErrorType.GENERIC_ERROR);
        } catch (SecurityException e) {
            log.error("SecurityException during path validation for filename '{}' in {}: {}", filename,
                    baseStorageDirectory, e.getMessage(), e);
            throw new DAOException(
                    "Failed to validate path due to security restrictions: " + e.getMessage(), e,
                    DAOErrorType.GENERIC_ERROR);
        }
    }
}
