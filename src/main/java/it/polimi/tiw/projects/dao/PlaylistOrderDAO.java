package it.polimi.tiw.projects.dao;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.polimi.tiw.projects.exceptions.DAOException;
import it.polimi.tiw.projects.exceptions.DAOException.DAOErrorType;
import it.polimi.tiw.projects.utils.ObjectMapperUtils;
import it.polimi.tiw.projects.utils.StorageUtils;
import jakarta.validation.constraints.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;

public class PlaylistOrderDAO {
    private static final Logger log = LoggerFactory.getLogger(PlaylistOrderDAO.class);

    private static final String PLAYLIST_ORDERS_SUBFOLDER = "playlist_orders";
    private static final String JSON_EXTENSION = ".json";

    private final Path playlistOrdersStorageDirectory;
    private final ObjectMapper objectMapper;

    /**
     * Constructs a PlaylistOrderDAO with a specified base storage directory. The
     * 'playlist_orders' subdirectory within this base directory will be created if
     * it doesn't exist.
     *
     * @param baseStorageDirectory The Path object representing the base directory.
     * @throws RuntimeException if the 'playlist_orders' subdirectory cannot be
     *                          created.
     */
    public PlaylistOrderDAO(@NotNull Path baseStorageDirectory) {
        this.playlistOrdersStorageDirectory = baseStorageDirectory.resolve(PLAYLIST_ORDERS_SUBFOLDER).normalize();
        this.objectMapper = ObjectMapperUtils.getMapper();

        try {
            Files.createDirectories(this.playlistOrdersStorageDirectory);
            log.info("PlaylistOrderDAO initialized. Storage directory: {}", this.playlistOrdersStorageDirectory);
        } catch (IOException e) {
            log.error("CRITICAL: Could not create playlist_orders storage directory: {}",
                    this.playlistOrdersStorageDirectory, e);
            throw new RuntimeException("Could not initialize playlist_orders storage directory", e);
        } catch (SecurityException e) {
            log.error("CRITICAL: Security permissions prevent creating playlist_orders storage directory: {}",
                    this.playlistOrdersStorageDirectory, e);
            throw new RuntimeException("Security permissions prevent creating playlist_orders storage directory", e);
        }
    }

    /**
     * Saves or updates the custom order for a given playlist. The method expects
     * orderedSongIds to represent the desired unique sequence.
     *
     * @param idPlaylist     The ID of the playlist.
     * @param orderedSongIds A list of song IDs in the desired order.
     * @throws DAOException If an I/O error occurs or if inputs are invalid.
     */
    public void savePlaylistOrder(int idPlaylist, @NotNull List<Integer> orderedSongIds) throws DAOException {
        log.info("Attempting to save order for playlist ID: {}", idPlaylist);
        if (idPlaylist <= 0) {
            log.warn("Save playlist order failed: Invalid playlist ID {}.", idPlaylist);
            throw new DAOException("Playlist ID must be positive.", DAOErrorType.GENERIC_ERROR);
        }
        if (orderedSongIds == null) {
            log.warn("Save playlist order failed for playlist ID {}: orderedSongIds is null.", idPlaylist);
            throw new DAOException("Ordered song IDs list cannot be null.", DAOErrorType.GENERIC_ERROR);
        }

        String filename = idPlaylist + JSON_EXTENSION;
        Path filePath = this.playlistOrdersStorageDirectory.resolve(filename);

        try {
            if (!filePath.normalize().startsWith(this.playlistOrdersStorageDirectory.normalize())) {
                log.warn(
                        "Security alert: Attempt to write playlist order outside designated directory. Playlist ID: {}, Path: {}",
                        idPlaylist, filePath);
                throw new DAOException("Attempt to write playlist order outside designated directory.",
                        DAOErrorType.ACCESS_DENIED);
            }

            byte[] jsonBytes = objectMapper.writeValueAsBytes(orderedSongIds);
            Files.write(filePath, jsonBytes, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING,
                    StandardOpenOption.WRITE);
            log.info("Successfully saved order for playlist ID: {} to {}", idPlaylist, filePath);
        } catch (JsonProcessingException e) {
            log.error("Error serializing playlist order for playlist ID {}: {}", idPlaylist, e.getMessage(), e);
            throw new DAOException("Failed to serialize playlist order to JSON.", e, DAOErrorType.GENERIC_ERROR);
        } catch (IOException e) {
            log.error("IOException occurred while saving playlist order for playlist ID {}: {}", idPlaylist,
                    e.getMessage(), e);
            throw new DAOException("Failed to save playlist order due to I/O error.", e, DAOErrorType.GENERIC_ERROR);
        } catch (SecurityException e) {
            log.error("SecurityException occurred while saving playlist order for playlist ID {}: {}", idPlaylist,
                    e.getMessage(), e);
            throw new DAOException("Failed to save playlist order due to security restrictions.", e,
                    DAOErrorType.GENERIC_ERROR);
        }
    }

    /**
     * Retrieves the custom order for a given playlist.
     *
     * @param idPlaylist The ID of the playlist.
     * @return A list of song IDs in custom order, or null if no custom order
     * exists.
     * @throws DAOException If an I/O error occurs or if the playlist ID is invalid.
     */
    public @NotNull List<Integer> getPlaylistOrder(int idPlaylist) throws DAOException {
        log.info("Attempting to retrieve order for playlist ID: {}", idPlaylist);
        if (idPlaylist <= 0) {
            log.warn("Get playlist order failed: Invalid playlist ID {}.", idPlaylist);
            throw new DAOException("Playlist ID must be positive.", DAOErrorType.GENERIC_ERROR);
        }

        String filename = idPlaylist + JSON_EXTENSION;

        try {
            Path resolvedFilePath = StorageUtils.validateAndResolveSecurePath(filename,
                    this.playlistOrdersStorageDirectory);

            List<Integer> orderedSongIds = objectMapper.readValue(resolvedFilePath.toFile(), new TypeReference<>() {
            });
            log.info("Successfully retrieved order for playlist ID: {}", idPlaylist);
            return orderedSongIds;
        } catch (DAOException e) {
            if (e.getErrorType() == DAOErrorType.NOT_FOUND) {
                log.info("No custom order file found for playlist ID: {}. Returning empty list.", idPlaylist);
                return List.of();
            }
            log.error("DAOException occurred while retrieving playlist order for playlist ID {}: {}", idPlaylist,
                    e.getMessage(), e);
            throw e;
        } catch (IllegalArgumentException e) {
            log.warn("Get playlist order failed for playlist ID {}: Invalid filename for StorageUtils {}. Error: {}",
                    idPlaylist, filename, e.getMessage());
            throw new DAOException("Invalid filename for retrieving playlist order.", e, DAOErrorType.GENERIC_ERROR);
        } catch (JsonProcessingException e) {
            log.error("Error deserializing playlist order for playlist ID {}: {}", idPlaylist, e.getMessage(), e);
            throw new DAOException("Failed to deserialize playlist order from JSON.", e, DAOErrorType.GENERIC_ERROR);
        } catch (IOException e) {
            log.error("IOException occurred while retrieving playlist order for playlist ID {}: {}", idPlaylist,
                    e.getMessage(), e);
            throw new DAOException("Failed to retrieve playlist order due to I/O error.", e,
                    DAOErrorType.GENERIC_ERROR);
        } catch (SecurityException e) {
            log.error("SecurityException occurred while retrieving playlist order for playlist ID {}: {}", idPlaylist,
                    e.getMessage(), e);
            throw new DAOException("Failed to retrieve playlist order due to security restrictions.", e,
                    DAOErrorType.GENERIC_ERROR);
        }
    }

    /**
     * Deletes the custom order file for a given playlist.
     *
     * @param idPlaylist The ID of the playlist whose custom order file should be
     *                   deleted.
     * @throws DAOException If an I/O error occurs or if the playlist ID is invalid.
     */
    public void deletePlaylistOrder(int idPlaylist) throws DAOException {
        log.info("Attempting to delete order file for playlist ID: {}", idPlaylist);
        if (idPlaylist <= 0) {
            log.warn("Delete playlist order failed: Invalid playlist ID {}.", idPlaylist);
            throw new DAOException("Playlist ID must be positive.", DAOErrorType.GENERIC_ERROR);
        }

        String filename = idPlaylist + JSON_EXTENSION;
        Path filePath = this.playlistOrdersStorageDirectory.resolve(filename).normalize();

        try {
            if (!filePath.startsWith(this.playlistOrdersStorageDirectory.normalize())) {
                log.warn(
                        "Security alert: Attempt to delete playlist order file outside designated directory. Playlist ID: {}, Path: {}",
                        idPlaylist, filePath);
                throw new DAOException("Attempt to delete playlist order file outside designated directory.",
                        DAOErrorType.ACCESS_DENIED);
            }

            boolean deleted = Files.deleteIfExists(filePath);
            if (deleted) {
                log.info("Successfully deleted order file for playlist ID: {}", idPlaylist);
            } else {
                log.info("No order file found to delete for playlist ID: {} (or already deleted).", idPlaylist);
            }
        } catch (IOException e) {
            log.error("IOException occurred while deleting playlist order file for playlist ID {}: {}", idPlaylist,
                    e.getMessage(), e);
            throw new DAOException("Failed to delete playlist order file due to I/O error.", e,
                    DAOErrorType.GENERIC_ERROR);
        } catch (SecurityException e) {
            log.error("SecurityException occurred while deleting playlist order file for playlist ID {}: {}",
                    idPlaylist, e.getMessage(), e);
            throw new DAOException("Failed to delete playlist order file due to security restrictions.", e,
                    DAOErrorType.GENERIC_ERROR);
        }
    }
}
