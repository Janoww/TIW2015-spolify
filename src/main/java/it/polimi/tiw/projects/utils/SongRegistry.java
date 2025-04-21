package it.polimi.tiw.projects.utils;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

import it.polimi.tiw.projects.beans.Song;

/**
 * A registry to hold and provide access to Song objects globally. This acts as
 * an in-memory cache, typically initialized at application startup.
 */
public class SongRegistry {

	// Using ConcurrentHashMap for thread safety, although initialization should
	// happen once.
	// Marked as final so the map reference itself cannot be changed after
	// initialization.
	private static final Map<Integer, Song> songMap = new ConcurrentHashMap<>();
	private static boolean initialized = false;

	/**
	 * Initializes the registry with a collection of songs. This method should only
	 * be called once, typically at application startup. It converts the list to a
	 * map and makes it unmodifiable to prevent runtime changes.
	 *
	 * @param initialSongs The initial list of songs to populate the registry.
	 * @throws IllegalStateException if the registry has already been initialized.
	 */
	public static synchronized void initialize(Collection<Song> initialSongs) {
		if (initialized) {
			throw new IllegalStateException("SongRegistry already initialized.");
		}
		if (initialSongs != null) {
			// Populate the ConcurrentHashMap directly
			songMap.putAll(initialSongs.stream().collect(Collectors.toMap(Song::getIdSong, Function.identity())));
		}
		initialized = true;
		System.out.println("SongRegistry initialized with " + songMap.size() + " songs.");
	}

	/**
	 * Retrieves a Song by its ID.
	 *
	 * @param id The ID of the song to retrieve.
	 * @return The Song object, or null if no song with the given ID exists.
	 * @throws IllegalStateException if the registry has not been initialized.
	 */
	public static Song getSongById(int id) {
		if (!initialized) {
			throw new IllegalStateException("SongRegistry not initialized.");
		}
		return songMap.get(id);
	}

	/**
	 * Retrieves all songs stored in the registry. Returns an unmodifiable view of
	 * the values to prevent external modification.
	 *
	 * @return An unmodifiable collection of all songs.
	 * @throws IllegalStateException if the registry has not been initialized.
	 */
	public static Collection<Song> getAllSongs() {
		if (!initialized) {
			throw new IllegalStateException("SongRegistry not initialized.");
		}
		// Return an unmodifiable view of the values
		return Collections.unmodifiableCollection(songMap.values());
	}

	/**
	 * Adds a new song to the registry. If a song with the same ID already exists,
	 * an exception is thrown.
	 *
	 * @param song The Song object to add.
	 * @return {@code true} if the song was added successfully, {@code false} if the
	 *         song is null or a song with the same ID already exists.
	 * @throws IllegalStateException if the registry has not been initialized.
	 */
	public static boolean addSong(Song song) {
		if (!initialized) {
			throw new IllegalStateException("SongRegistry not initialized.");
		}
		if (song == null) {
			System.err.println("Attempted to add a null song.");
			return false; // Cannot add a null song.
		}
		return songMap.putIfAbsent(song.getIdSong(), song) == null;
	}

	/**
	 * Removes a song from the registry by its ID.
	 *
	 * @param songId The ID of the song to remove.
	 * @return The removed Song object, or null if no song with the given ID was
	 *         found.
	 * @throws IllegalStateException if the registry has not been initialized.
	 */
	public static Song removeSong(int songId) {
		if (!initialized) {
			throw new IllegalStateException("SongRegistry not initialized.");
		}
		Song removedSong = songMap.remove(songId);
		return removedSong;
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
	 * Resets the registry to its initial state. Primarily intended for testing
	 * purposes to allow re-initialization.
	 */
	public static synchronized void reset() {
		songMap.clear();
		initialized = false;
		System.out.println("SongRegistry reset.");
	}
}
