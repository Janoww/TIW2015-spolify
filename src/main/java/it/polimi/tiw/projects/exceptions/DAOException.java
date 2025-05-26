package it.polimi.tiw.projects.exceptions;

public class DAOException extends Exception {

    private final DAOErrorType errorType;

    public DAOException(String message, DAOErrorType errorType) {
        super(message);
        this.errorType = errorType;
    }

    public DAOException(String message, Throwable cause, DAOErrorType errorType) {
        super(message, cause);
        this.errorType = errorType;
    }

    public DAOErrorType getErrorType() {
        return errorType;
    }

    public enum DAOErrorType {
        NAME_ALREADY_EXISTS, // e.g., User username, Album/Playlist name per user
        INVALID_CREDENTIALS, // e.g., Login failure
        NOT_FOUND, // e.g., Entity lookup by ID failed, FK target doesn't exist
        DUPLICATE_ENTRY, // e.g., Adding a song already in a playlist
        ACCESS_DENIED, // e.g., Operation on resource not owned by user
        CONSTRAINT_VIOLATION, // e.g., Other integrity constraint issues
        AUDIO_SAVE_FAILED, // Error during audio file saving in a workflow
        IMAGE_SAVE_FAILED, // Error during image file saving in a workflow
        GENERIC_ERROR // For other potential DAO errors
    }
}
