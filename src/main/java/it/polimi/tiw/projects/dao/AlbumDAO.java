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
     * @throws SQLException if a database access error occurs.
     */
    public Album createAlbum(String name, int year, String artist) throws SQLException {
        String query = "INSERT into Album (name, year, artist) VALUES(?, ?, ?)";
        Album newAlbum = null;
        ResultSet generatedKeys = null;

        // Use RETURN_GENERATED_KEYS to get the new album ID
        try (PreparedStatement pStatement = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            pStatement.setString(1, name);
            pStatement.setInt(2, year);
            pStatement.setString(3, artist);
            int affectedRows = pStatement.executeUpdate();

            if (affectedRows == 0) {
                throw new SQLException("Creating album failed, no rows affected.");
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
                throw new SQLException("Creating album failed, no ID obtained.");
            }
        } finally {
            if (generatedKeys != null)
                try {
                    generatedKeys.close();
                } catch (SQLException e) {
                    // Log or ignore
                }
        }
        return newAlbum;
    }

    /**
     * Finds an album by its ID.
     *
     * @param idAlbum The ID of the album to find.
     * @return The Album object if found, null otherwise.
     * @throws SQLException if a database access error occurs.
     */
    public Album findAlbumById(int idAlbum) throws SQLException {
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
        }
        return album;
    }

    /**
     * Finds all albums in the database.
     *
     * @return A list of all albums.
     * @throws SQLException if a database access error occurs.
     */
    public List<Album> findAllAlbums() throws SQLException {
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
     * @throws SQLException             if a database access error occurs.
     * @throws IllegalArgumentException if all update parameters (name, year,
     *                                  artist) are null.
     */
    public boolean updateAlbum(int idAlbum, String name, Integer year, String artist) throws SQLException {
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
            return affectedRows > 0;
        }
    }

    /**
     * Deletes an album from the database by its ID.
     *
     * @param idAlbum The ID of the album to delete.
     * @return true if the deletion was successful, false otherwise.
     * @throws SQLException if a database access error occurs.
     */
    public boolean deleteAlbum(int idAlbum) throws SQLException {
        String query = "DELETE FROM Album WHERE idAlbum = ?";
        try (PreparedStatement pStatement = connection.prepareStatement(query)) {
            pStatement.setInt(1, idAlbum);
            int affectedRows = pStatement.executeUpdate();
            return affectedRows > 0;
        }
    }
}
