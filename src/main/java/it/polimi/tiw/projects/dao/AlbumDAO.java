package it.polimi.tiw.projects.dao;

import it.polimi.tiw.projects.beans.Album;
import it.polimi.tiw.projects.exceptions.DAOException;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AlbumDAO {
    private Connection connection;

    public AlbumDAO(Connection connection) {
        this.connection = connection;
    }

    /**
     * Creates a new album in the database.
     *
     * @param name   The name of the album.
     * @param year   The release year of the album.
     * @param artist The artist of the album.
     * @return The newly created Album object with its generated ID.
     * @throws DAOException if a database access error occurs or the name already
     *                      exists.
     */
    public Album createAlbum(String name, int year, String artist) throws DAOException {
        String query = "INSERT into Album (name, year, artist) VALUES(?, ?, ?)";
        Album newAlbum = null;
        ResultSet generatedKeys = null;

        try (PreparedStatement pStatement = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            pStatement.setString(1, name);
            pStatement.setInt(2, year);
            pStatement.setString(3, artist);
            int affectedRows = pStatement.executeUpdate();

            if (affectedRows == 0) {
                // This case might not happen with auto-increment keys but kept for robustness
                throw new DAOException("Creating album failed, no rows affected.",
                        DAOException.DAOErrorType.GENERIC_ERROR);
            }

            generatedKeys = pStatement.getGeneratedKeys();
            if (generatedKeys.next()) {
                int newId = generatedKeys.getInt(1);
                // Create the Album bean
                newAlbum = new Album();
                newAlbum.setIdAlbum(newId);
                newAlbum.setName(name);
                newAlbum.setYear(year);
                newAlbum.setArtist(artist);
            } else {
                throw new DAOException("Creating album failed, no ID obtained.",
                        DAOException.DAOErrorType.GENERIC_ERROR);
            }
        } catch (SQLException e) {
            // Check for unique constraint violation (MySQL error code 1062, SQLState
            // '23000')
            if ("23000".equals(e.getSQLState())) {
                throw new DAOException("Album name '" + name + "' already exists.", e,
                        DAOException.DAOErrorType.NAME_ALREADY_EXISTS);
            } else {
                throw new DAOException("Error creating album: " + e.getMessage(), e,
                        DAOException.DAOErrorType.GENERIC_ERROR);
            }
        } finally {
            try {
                if (generatedKeys != null)
                    generatedKeys.close();
            } catch (SQLException e) {
                System.err.println("Failed to close ResultSet: " + e.getMessage());
                throw new DAOException("Failed to close resources", e, DAOException.DAOErrorType.GENERIC_ERROR);
            }
        }
        return newAlbum;
    }

    /**
     * Finds an album by its ID.
     *
     * @param idAlbum The ID of the album to find.
     * @return The Album object if found, null otherwise.
     * @throws DAOException if a database access error occurs.
     */
    public Album findAlbumById(int idAlbum) throws DAOException {
        Album album = null;
        String query = "SELECT idAlbum, name, year, artist FROM Album WHERE idAlbum = ?";
        try (PreparedStatement pStatement = connection.prepareStatement(query)) {
            pStatement.setInt(1, idAlbum);
            try (ResultSet result = pStatement.executeQuery()) {
                if (result.next()) {
                    album = new Album();
                    album.setIdAlbum(result.getInt("idAlbum"));
                    album.setName(result.getString("name"));
                    album.setYear(result.getInt("year"));
                    album.setArtist(result.getString("artist"));
                }
            }
        } catch (SQLException e) {
            throw new DAOException("Error finding album by ID: " + e.getMessage(), e,
                    DAOException.DAOErrorType.GENERIC_ERROR);
        }
        return album;
    }

    /**
     * Finds all albums in the database.
     *
     * @return A list of all albums.
     * @throws DAOException if a database access error occurs.
     */
    public List<Album> findAllAlbums() throws DAOException {
        List<Album> albums = new ArrayList<>();
        String query = "SELECT idAlbum, name, year, artist FROM Album ORDER BY artist, year, name";
        try (Statement statement = connection.createStatement();
                ResultSet result = statement.executeQuery(query)) {
            while (result.next()) {
                Album album = new Album();
                album.setIdAlbum(result.getInt("idAlbum"));
                album.setName(result.getString("name"));
                album.setYear(result.getInt("year"));
                album.setArtist(result.getString("artist"));
                albums.add(album);
            }
        } catch (SQLException e) {
            throw new DAOException("Error finding all albums: " + e.getMessage(), e,
                    DAOException.DAOErrorType.GENERIC_ERROR);
        }
        return albums;
    }

    /**
     * Updates an existing album in the database, only modifying fields with
     * non-null values.
     *
     * @param idAlbum The ID of the album to update.
     * @param name    The new name for the album (or null to keep existing).
     * @param year    The new release year for the album (or null to keep existing).
     * @param artist  The new artist for the album (or null to keep existing).
     * @return true if the update was successful (at least one field updated), false
     *         otherwise.
     * @throws DAOException             if a database access error occurs or the new
     *                                  name already exists.
     * @throws IllegalArgumentException if all update parameters (name, year,
     *                                  artist) are null.
     */
    public boolean updateAlbum(int idAlbum, String name, Integer year, String artist) throws DAOException {
        // Build the query dynamically
        StringBuilder queryBuilder = new StringBuilder("UPDATE Album SET ");
        List<Object> params = new ArrayList<>();
        boolean firstField = true;

        if (name != null) {
            queryBuilder.append("name = ?");
            params.add(name);
            firstField = false;
        }
        if (year != null) {
            if (!firstField)
                queryBuilder.append(", ");
            queryBuilder.append("year = ?");
            params.add(year);
            firstField = false;
        }
        if (artist != null) {
            if (!firstField)
                queryBuilder.append(", ");
            queryBuilder.append("artist = ?");
            params.add(artist);
            firstField = false;
        }

        // Check if any field was actually added for update
        if (params.isEmpty()) {
            // Or return false, or throw an exception, depending on desired behavior
            // Throwing exception seems more appropriate to signal incorrect usage
            throw new IllegalArgumentException("No fields provided for update.");
        }

        queryBuilder.append(" WHERE idAlbum = ?");
        params.add(idAlbum);

        String query = queryBuilder.toString();

        try (PreparedStatement pStatement = connection.prepareStatement(query)) {
            // Set parameters dynamically
            for (int i = 0; i < params.size(); i++) {
                Object param = params.get(i);
                if (param instanceof String) {
                    pStatement.setString(i + 1, (String) param);
                } else if (param instanceof Integer) {
                    pStatement.setInt(i + 1, (Integer) param);
                }
            }

            int affectedRows = pStatement.executeUpdate();
            if (affectedRows == 0) {
                throw new DAOException("Album with ID " + idAlbum + " not found for update.",
                        DAOException.DAOErrorType.NOT_FOUND);
            }
            return true; // If we reach here, affectedRows must be > 0
        } catch (SQLException e) {
            // Check for unique constraint violation (MySQL error code 1062, SQLState
            // '23000')
            if ("23000".equals(e.getSQLState()) && name != null) { // Check if name was part of the update
                throw new DAOException("Album name '" + name + "' already exists.", e,
                        DAOException.DAOErrorType.NAME_ALREADY_EXISTS);
            } else {
                throw new DAOException("Error updating album: " + e.getMessage(), e,
                        DAOException.DAOErrorType.GENERIC_ERROR);
            }
        }
    }

    /**
     * Deletes an album from the database by its ID.
     *
     * @param idAlbum The ID of the album to delete.
     * @return true if the deletion was successful, false otherwise.
     * @throws DAOException if a database access error occurs.
     */
    public boolean deleteAlbum(int idAlbum) throws DAOException {
        String query = "DELETE FROM Album WHERE idAlbum = ?";
        try (PreparedStatement pStatement = connection.prepareStatement(query)) {
            pStatement.setInt(1, idAlbum);
            int affectedRows = pStatement.executeUpdate();
            if (affectedRows == 0) {
                throw new DAOException("Album with ID " + idAlbum + " not found for deletion.",
                        DAOException.DAOErrorType.NOT_FOUND);
            }
            return true; // If we reach here, affectedRows must be > 0
        } catch (SQLException e) {
            throw new DAOException("Error deleting album: " + e.getMessage(), e,
                    DAOException.DAOErrorType.GENERIC_ERROR);
        }
    }
}
