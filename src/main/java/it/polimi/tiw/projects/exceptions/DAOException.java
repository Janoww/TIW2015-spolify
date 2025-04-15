package it.polimi.tiw.projects.exceptions;

public class DAOException extends Exception {

    private static final long serialVersionUID = 1L;

    public enum DAOErrorType {
        USERNAME_ALREADY_EXISTS,
        INVALID_CREDENTIALS,
        NOT_FOUND,
        GENERIC_ERROR // For other potential DAO errors
    }

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
}
