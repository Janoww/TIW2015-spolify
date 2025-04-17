package it.polimi.tiw.projects.utils;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

import it.polimi.tiw.projects.beans.Album;

/**
 * A registry to hold and provide access to Album objects globally.
 * This acts as an in-memory cache, typically initialized at application
 * startup.
 */
public class AlbumRegistry {

    // Using ConcurrentHashMap for thread safety, although initialization should
    // happen once.
    // Marked as final so the map reference itself cannot be changed after
    // initialization.
    private static final Map<Integer, Album> albumMap = new ConcurrentHashMap<>();
    private static boolean initialized = false;

    /**
     * Initializes the registry with a collection of albums.
     * This method should only be called once, typically at application startup.
     * It converts the list to a map and makes it unmodifiable to prevent runtime
     * changes.
     *
     * @param initialAlbums The initial list of albums to populate the registry.
     * @throws IllegalStateException if the registry has already been initialized.
     */
    public static synchronized void initialize(Collection<Album> initialAlbums) {
        if (initialized) {
            throw new IllegalStateException("AlbumRegistry already initialized.");
        }
        if (initialAlbums != null) {
            // Populate the ConcurrentHashMap directly
            albumMap.putAll(initialAlbums.stream()
                    .collect(Collectors.toMap(Album::getIdAlbum, Function.identity())));
        }
        initialized = true;
        System.out.println("AlbumRegistry initialized with " + albumMap.size() + " albums.");
    }

    /**
     * Retrieves an Album by its ID.
     *
     * @param id The ID of the album to retrieve.
     * @return The Album object, or null if no album with the given ID exists.
     * @throws IllegalStateException if the registry has not been initialized.
     */
    public static Album getAlbumById(int id) {
        if (!initialized) {
            throw new IllegalStateException("AlbumRegistry not initialized.");
        }
        return albumMap.get(id);
    }

    /**
     * Retrieves all albums stored in the registry.
     * Returns an unmodifiable view of the values to prevent external modification.
     *
     * @return An unmodifiable collection of all albums.
     * @throws IllegalStateException if the registry has not been initialized.
     */
    public static Collection<Album> getAllAlbums() {
        if (!initialized) {
            throw new IllegalStateException("AlbumRegistry not initialized.");
        }
        // Return an unmodifiable view of the values
        return Collections.unmodifiableCollection(albumMap.values());
    }

    /**
     * Adds a new album to the registry.
     * If an album with the same ID already exists, an exception is thrown.
     *
     * @param album The Album object to add.
     * @return {@code true} if the album was added successfully, {@code false} if
     *         the album is null or an album with the same ID already exists.
     * @throws IllegalStateException if the registry has not been initialized.
     */
    public static boolean addAlbum(Album album) {
        if (!initialized) {
            throw new IllegalStateException("AlbumRegistry not initialized.");
        }
        if (album == null) {
            System.err.println("Attempted to add a null album.");
            return false; // Cannot add a null album.
        }
        return albumMap.putIfAbsent(album.getIdAlbum(), album) == null;
    }

    /**
     * Removes an album from the registry by its ID.
     *
     * @param albumId The ID of the album to remove.
     * @return The removed Album object, or null if no album with the given ID was
     *         found.
     * @throws IllegalStateException if the registry has not been initialized.
     */
    public static Album removeAlbum(int albumId) {
        if (!initialized) {
            throw new IllegalStateException("AlbumRegistry not initialized.");
        }
        Album removedAlbum = albumMap.remove(albumId);
        return removedAlbum;
    }

    /**
     * Checks if the registry has been initialized.
     * 
     * @return true if initialized, false otherwise.
     */
    public static boolean isInitialized() {
        return initialized;
    }

    /**
     * Resets the registry to its initial state.
     * Primarily intended for testing purposes to allow re-initialization.
     */
    public static synchronized void reset() {
        albumMap.clear();
        initialized = false;
        System.out.println("AlbumRegistry reset.");
    }
}
