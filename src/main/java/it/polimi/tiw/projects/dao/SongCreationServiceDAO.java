package it.polimi.tiw.projects.dao;

import it.polimi.tiw.projects.beans.*;
import it.polimi.tiw.projects.exceptions.DAOException;
import jakarta.servlet.http.Part;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public class SongCreationServiceDAO {
    private static final Logger logger = LoggerFactory.getLogger(SongCreationServiceDAO.class);

    private final Connection connection;
    private final AlbumDAO albumDAO;
    private final SongDAO songDAO;
    private final ImageDAO imageDAO;
    private final AudioDAO audioDAO;

    public SongCreationServiceDAO(@NotNull Connection connection, @NotNull AlbumDAO albumDAO, @NotNull SongDAO songDAO,
            @NotNull ImageDAO imageDAO, @NotNull AudioDAO audioDAO) {
        this.connection = connection;
        this.albumDAO = albumDAO;
        this.songDAO = songDAO;
        this.imageDAO = imageDAO;
        this.audioDAO = audioDAO;
    }

    private String saveAudioFile(@NotNull User user, @NotNull SongCreationParameters params) throws DAOException {
        String audioFileStorageName;
        try {
            String originalAudioFileName = Paths.get(params.audioFilePart().getSubmittedFileName()).getFileName()
                    .toString();

            try (InputStream audioContent = params.audioFilePart().getInputStream()) {
                audioFileStorageName = audioDAO.saveAudio(audioContent, originalAudioFileName);
            }
            logger.info("Audio file {} saved as {} for user {}", originalAudioFileName, audioFileStorageName,
                    user.getUsername());
            return audioFileStorageName;
        } catch (IOException e) {
            throw new DAOException("Failed to process audio file: " + e.getMessage(), e,
                    DAOException.DAOErrorType.AUDIO_SAVE_FAILED);
        } catch (DAOException e) {
            throw new DAOException("Failed to save audio file: " + e.getMessage(), e,
                    DAOException.DAOErrorType.AUDIO_SAVE_FAILED);
        }
    }

    private String saveAlbumImageFile(@NotNull User user, @NotBlank String albumTitle, Part imageFilePart)
            throws DAOException {
        String imageFileStorageName = null;
        if (imageFilePart != null && imageFilePart.getSize() > 0) {
            try {
                String originalImageFileName = Paths.get(imageFilePart.getSubmittedFileName()).getFileName().toString();
                try (InputStream imageContent = imageFilePart.getInputStream()) {
                    imageFileStorageName = imageDAO.saveImage(imageContent, originalImageFileName);
                }
                logger.info("Image file {} saved as {} for new album {} by user {}", originalImageFileName,
                        imageFileStorageName, albumTitle, user.getUsername());
            } catch (IOException e) {
                throw new DAOException("Failed to process album image file: " + e.getMessage(), e,
                        DAOException.DAOErrorType.IMAGE_SAVE_FAILED);
            } catch (DAOException e) {
                throw new DAOException("Failed to save album image file: " + e.getMessage(), e,
                        DAOException.DAOErrorType.IMAGE_SAVE_FAILED);
            }
        }
        return imageFileStorageName;
    }

    private AlbumData handleAlbumProcessing(@NotNull User user, @NotNull SongCreationParameters params,
            Part imageFilePart) throws DAOException {
        Album album;
        String imageFileStorageName = null;
        boolean newAlbumCreated = false;

        List<Album> userAlbums = albumDAO.findAlbumsByUser(user.getIdUser());
        album = userAlbums.stream().filter(x -> x.getName().equalsIgnoreCase(params.albumTitle())).findFirst()
                .orElse(null);

        if (album == null) {
            newAlbumCreated = true;
            logger.info("No album named '{}' found for user {}. Attempting to create new album.", params.albumTitle(),
                    user.getUsername());

            imageFileStorageName = saveAlbumImageFile(user, params.albumTitle(), imageFilePart);

            album = albumDAO.createAlbum(params.albumTitle(), params.albumYear(), params.albumArtist(),
                    imageFileStorageName, user.getIdUser());
            logger.info("New album '{}' (ID: {}) created for user {}", album.getName(), album.getIdAlbum(),
                    user.getUsername());
        } else {
            logger.info("Found existing album '{}' (ID: {}) for user {}.", album.getName(), album.getIdAlbum(),
                    user.getUsername());
        }
        return new AlbumData(album, imageFileStorageName, newAlbumCreated);
    }

    private Song saveSongDetails(@NotNull User user, @NotNull SongCreationParameters params, int albumId,
            @NotBlank String audioFileStorageName) throws DAOException {
        Song createdSong = songDAO.createSong(params.songTitle(), albumId, params.genre(), audioFileStorageName,
                user.getIdUser());
        logger.info("Song '{}' (ID: {}) created and associated with album ID {} for user {}", createdSong.getTitle(),
                createdSong.getIdSong(), albumId, user.getUsername());
        return createdSong;
    }

    private void performCleanupOnFailure(String audioFileStorageName, AlbumData albumInfo, @NotBlank String username) {
        if (audioFileStorageName != null) {
            try {
                logger.warn("Attempting to delete audio file {} after failed workflow for user {}",
                        audioFileStorageName, username);
                audioDAO.deleteAudio(audioFileStorageName);
                logger.info("Successfully deleted audio file {} after failed workflow for user {}.",
                        audioFileStorageName, username);
            } catch (DAOException | IllegalArgumentException dae) {
                logger.error("Failed to delete audio file {} during cleanup for user {}: {}", audioFileStorageName,
                        username, dae.getMessage(), dae);
            }
        }
        if (albumInfo != null && albumInfo.newAlbumCreated && albumInfo.imageFileStorageName != null) {
            try {
                logger.warn("Attempting to delete image file {} after failed workflow for user {}",
                        albumInfo.imageFileStorageName, username);
                imageDAO.deleteImage(albumInfo.imageFileStorageName);
                logger.info("Successfully deleted image file {} after failed workflow for user {}.",
                        albumInfo.imageFileStorageName, username);
            } catch (DAOException | IllegalArgumentException dae) {
                logger.error("Failed to delete image file {} during cleanup for user {}: {}",
                        albumInfo.imageFileStorageName, username, dae.getMessage(), dae);
            }
        }
    }

    public @NotNull SongWithAlbum createSongWorkflow(@NotNull User user, @NotNull SongCreationParameters params,
            Part imageFilePart) throws DAOException {

        String audioFileStorageName = null;
        AlbumData albumInfo = null;
        logger.info("Starting song creation workflow for user {}", user.getUsername());

        try {
            connection.setAutoCommit(false);
            logger.debug("Transaction started for song creation workflow by user: {}", user.getUsername());

            audioFileStorageName = saveAudioFile(user, params);
            albumInfo = handleAlbumProcessing(user, params, imageFilePart);
            Song createdSong = saveSongDetails(user, params, albumInfo.album.getIdAlbum(), audioFileStorageName);

            connection.commit();
            logger.info("Transaction committed successfully for song creation workflow by user: {}",
                    user.getUsername());

            return new SongWithAlbum(createdSong, albumInfo.album);

        } catch (DAOException | SQLException e) {
            logger.error("Error during song creation workflow for user {}. Attempting rollback. Error: {}",
                    user.getUsername(), e.getMessage(), e);
            try {
                connection.rollback();
                logger.info("Transaction rolled back for user {}", user.getUsername());
            } catch (SQLException exRollback) {
                logger.error("Rollback failed for user {} after error: {}", user.getUsername(), exRollback.getMessage(),
                        exRollback);
                e.addSuppressed(exRollback);
            }

            performCleanupOnFailure(audioFileStorageName, albumInfo, user.getUsername());

            if (e instanceof DAOException daoException) {
                throw daoException;
            } else {
                throw new DAOException("Database error during song creation workflow: " + e.getMessage(), e,
                        DAOException.DAOErrorType.GENERIC_ERROR);
            }

        } finally {
            try {
                if (connection != null) {
                    connection.setAutoCommit(true);
                    logger.debug("Connection autoCommit reset to true for user {}", user.getUsername());
                }
            } catch (SQLException exFinal) {
                logger.error("Failed to reset autoCommit to true for user {}: {}", user.getUsername(),
                        exFinal.getMessage(), exFinal);
            }
        }
    }

    private record AlbumData(Album album, String imageFileStorageName, boolean newAlbumCreated) {
    }
}
