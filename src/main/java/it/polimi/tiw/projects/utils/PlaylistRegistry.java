package it.polimi.tiw.projects.utils;

import it.polimi.tiw.projects.beans.Playlist;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * A registry to hold and provide access to Playlist objects globally. This acts
 * as an in-memory cache, typically initialized at application startup.
 */
public class PlaylistRegistry {

	// Using ConcurrentHashMap for thread safety, although initialization should
	// happen once.
	// Marked as final so the map reference itself cannot be changed after
	// initialization.
	private static final Map<Integer, Playlist> playlistMap = new ConcurrentHashMap<>();
	private static boolean initialized = false;

	/**
	 * Initializes the registry with a collection of playlists. This method should
	 * only be called once, typically at application startup. It converts the list
	 * to a map and makes it unmodifiable to prevent runtime changes.
	 *
	 * @param initialPlaylists The initial list of playlists to populate the
	 *                         registry.
	 * @throws IllegalStateException if the registry has already been initialized.
	 */
	public static synchronized void initialize(Collection<Playlist> initialPlaylists) {
		if (initialized) {
			throw new IllegalStateException("PlaylistRegistry already initialized.");
		}
		if (initialPlaylists != null) {
			// Populate the ConcurrentHashMap directly
			playlistMap.putAll(
					initialPlaylists.stream().collect(Collectors.toMap(Playlist::getIdPlaylist, Function.identity())));
		}
		initialized = true;
		System.out.println("PlaylistRegistry initialized with " + playlistMap.size() + " playlists.");
	}

	/**
	 * Retrieves a Playlist by its ID.
	 *
	 * @param id The ID of the playlist to retrieve.
	 * @return The Playlist object, or null if no playlist with the given ID exists.
	 * @throws IllegalStateException if the registry has not been initialized.
	 */
	public static Playlist getPlaylistById(int id) {
		if (!initialized) {
			throw new IllegalStateException("PlaylistRegistry not initialized.");
		}
		return playlistMap.get(id);
	}

	/**
	 * Retrieves all playlists stored in the registry. Returns an unmodifiable view
	 * of the values to prevent external modification.
	 *
	 * @return An unmodifiable collection of all playlists.
	 * @throws IllegalStateException if the registry has not been initialized.
	 */
	public static Collection<Playlist> getAllPlaylists() {
		if (!initialized) {
			throw new IllegalStateException("PlaylistRegistry not initialized.");
		}
		// Return an unmodifiable view of the values
		return Collections.unmodifiableCollection(playlistMap.values());
	}

	/**
	 * Adds a new playlist to the registry. If a playlist with the same ID already
	 * exists, an exception is thrown.
	 *
	 * @param playlist The Playlist object to add.
	 * @return {@code true} if the playlist was added successfully, {@code false} if
	 *         the playlist is null or a playlist with the same ID already exists.
	 * @throws IllegalStateException if the registry has not been initialized.
	 */
	public static boolean addPlaylist(Playlist playlist) {
		if (!initialized) {
			throw new IllegalStateException("PlaylistRegistry not initialized.");
		}
		if (playlist == null) {
			System.err.println("Attempted to add a null playlist.");
			return false; // Cannot add a null playlist.
		}
		return playlistMap.putIfAbsent(playlist.getIdPlaylist(), playlist) == null;
	}

	/**
	 * Removes a playlist from the registry by its ID.
	 *
	 * @param playlistId The ID of the playlist to remove.
	 * @return The removed Playlist object, or null if no playlist with the given ID
	 *         was found.
	 * @throws IllegalStateException if the registry has not been initialized.
	 */
	public static Playlist removePlaylist(int playlistId) {
		if (!initialized) {
			throw new IllegalStateException("PlaylistRegistry not initialized.");
		}
		Playlist removedPlaylist = playlistMap.remove(playlistId);
		return removedPlaylist;
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
		playlistMap.clear();
		initialized = false;
		System.out.println("PlaylistRegistry reset.");
	}
}
