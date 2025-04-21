package it.polimi.tiw.projects.utils;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import it.polimi.tiw.projects.beans.Song;

public class SongRegistryTest {

	private Song song1;
	private Song song2;

	@BeforeEach
	void setUp() {
		// Reset the registry before each test to ensure isolation
		SongRegistry.reset();

		// Define a dummy user ID
		UUID dummyUserId = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
		int dummyAlbumId = 1;
		int dummyYear = 2024;

		// Create sample songs
		song1 = new Song();
		song1.setIdSong(1);
		song1.setTitle("Song One");
		song1.setGenre("Pop");
		song1.setAudioFile("/audio/song1.mp3");
		song1.setIdAlbum(dummyAlbumId);
		song1.setYear(dummyYear);
		song1.setIdUser(dummyUserId);

		song2 = new Song();
		song2.setIdSong(2);
		song2.setTitle("Song Two");
		song2.setGenre("Rock");
		song2.setAudioFile("/audio/song2.mp3");
		song2.setIdAlbum(dummyAlbumId);
		song2.setYear(dummyYear);
		song2.setIdUser(dummyUserId);
	}

	@Test
	void testInitialize_Success() {
		assertFalse(SongRegistry.isInitialized(), "Registry should not be initialized initially.");
		List<Song> initialSongs = new ArrayList<>();
		initialSongs.add(song1);
		initialSongs.add(song2);

		SongRegistry.initialize(initialSongs);

		assertTrue(SongRegistry.isInitialized(), "Registry should be initialized after calling initialize.");
		assertEquals(2, SongRegistry.getAllSongs().size(), "Registry should contain 2 songs after initialization.");
		assertEquals(song1, SongRegistry.getSongById(1), "Should retrieve song1 by ID.");
		assertEquals(song2, SongRegistry.getSongById(2), "Should retrieve song2 by ID.");
	}

	@Test
	void testInitialize_NullList() {
		assertFalse(SongRegistry.isInitialized());
		SongRegistry.initialize(null);
		assertTrue(SongRegistry.isInitialized());
		assertTrue(SongRegistry.getAllSongs().isEmpty(), "Registry should be empty when initialized with null.");
	}

	@Test
	void testInitialize_EmptyList() {
		assertFalse(SongRegistry.isInitialized());
		SongRegistry.initialize(new ArrayList<>());
		assertTrue(SongRegistry.isInitialized());
		assertTrue(SongRegistry.getAllSongs().isEmpty(),
				"Registry should be empty when initialized with an empty list.");
	}

	@Test
	void testInitialize_AlreadyInitialized() {
		SongRegistry.initialize(Collections.emptyList()); // Initialize once
		assertTrue(SongRegistry.isInitialized());

		List<Song> secondInitializationList = new ArrayList<>();
		secondInitializationList.add(song1);

		// Attempt to initialize again
		IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
			SongRegistry.initialize(secondInitializationList);
		}, "Initializing an already initialized registry should throw IllegalStateException.");

		assertEquals("SongRegistry already initialized.", exception.getMessage());
		assertTrue(SongRegistry.getAllSongs().isEmpty(),
				"Registry content should not change on re-initialization attempt.");
	}

	@Test
	void testGetSongById_NotInitialized() {
		assertFalse(SongRegistry.isInitialized());
		IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
			SongRegistry.getSongById(1);
		}, "Getting a song before initialization should throw IllegalStateException.");
		assertEquals("SongRegistry not initialized.", exception.getMessage());
	}

	@Test
	void testGetSongById_Found() {
		List<Song> initialSongs = new ArrayList<>();
		initialSongs.add(song1);
		SongRegistry.initialize(initialSongs);

		Song foundSong = SongRegistry.getSongById(1);
		assertNotNull(foundSong, "Should find song1.");
		assertEquals(song1, foundSong, "Found song should be equal to song1.");
	}

	@Test
	void testGetSongById_NotFound() {
		List<Song> initialSongs = new ArrayList<>();
		initialSongs.add(song1);
		SongRegistry.initialize(initialSongs);

		Song foundSong = SongRegistry.getSongById(99); // Non-existent ID
		assertNull(foundSong, "Should return null for a non-existent song ID.");
	}

	@Test
	void testGetAllSongs_NotInitialized() {
		assertFalse(SongRegistry.isInitialized());
		IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
			SongRegistry.getAllSongs();
		}, "Getting all songs before initialization should throw IllegalStateException.");
		assertEquals("SongRegistry not initialized.", exception.getMessage());
	}

	@Test
	void testGetAllSongs_Success() {
		List<Song> initialSongs = new ArrayList<>();
		initialSongs.add(song1);
		initialSongs.add(song2);
		SongRegistry.initialize(initialSongs);

		Collection<Song> allSongs = SongRegistry.getAllSongs();
		assertNotNull(allSongs, "Returned collection should not be null.");
		assertEquals(2, allSongs.size(), "Returned collection should contain 2 songs.");
		assertTrue(allSongs.contains(song1), "Collection should contain song1.");
		assertTrue(allSongs.contains(song2), "Collection should contain song2.");
	}

	@Test
	void testGetAllSongs_IsUnmodifiable() {
		List<Song> initialSongs = new ArrayList<>();
		initialSongs.add(song1);
		SongRegistry.initialize(initialSongs);

		Collection<Song> allSongs = SongRegistry.getAllSongs();

		// Attempt to modify the returned collection
		Executable addAttempt = () -> allSongs.add(song2); // Create a new song instance if needed
		Executable removeAttempt = () -> allSongs.remove(song1);
		Executable clearAttempt = allSongs::clear;

		assertThrows(UnsupportedOperationException.class, addAttempt,
				"Should not be able to add to the returned collection.");
		assertThrows(UnsupportedOperationException.class, removeAttempt,
				"Should not be able to remove from the returned collection.");
		assertThrows(UnsupportedOperationException.class, clearAttempt,
				"Should not be able to clear the returned collection.");
	}

	@Test
	void testAddSong_NotInitialized() {
		assertFalse(SongRegistry.isInitialized());
		IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
			SongRegistry.addSong(song1);
		}, "Adding a song before initialization should throw IllegalStateException.");
		assertEquals("SongRegistry not initialized.", exception.getMessage());
	}

	@Test
	void testAddSong_Success() {
		SongRegistry.initialize(new ArrayList<>()); // Initialize empty
		assertTrue(SongRegistry.getAllSongs().isEmpty());

		boolean added1 = SongRegistry.addSong(song1);
		assertTrue(added1, "addSong should return true for the first song.");
		assertEquals(1, SongRegistry.getAllSongs().size(), "Registry should have 1 song after adding.");
		assertEquals(song1, SongRegistry.getSongById(1), "Should retrieve the added song.");

		boolean added2 = SongRegistry.addSong(song2);
		assertTrue(added2, "addSong should return true for the second song.");
		assertEquals(2, SongRegistry.getAllSongs().size(), "Registry should have 2 songs after adding another.");
		assertEquals(song2, SongRegistry.getSongById(2), "Should retrieve the second added song.");
	}

	// Removed testAddSong_Overwrite as addSong now throws an exception for
	// duplicates

	@Test
	void testAddSong_DuplicateId() {
		SongRegistry.initialize(List.of(song1)); // Initialize with song1
		assertEquals(1, SongRegistry.getAllSongs().size());

		// Create another song with the same ID
		Song song1Duplicate = new Song();
		song1Duplicate.setIdSong(1); // Same ID as song1
		song1Duplicate.setTitle("Song One Duplicate");
		song1Duplicate.setGenre("Jazz");
		song1Duplicate.setAudioFile("/audio/song1_dup.mp3");
		song1Duplicate.setIdAlbum(2);
		song1Duplicate.setYear(2025);
		song1Duplicate.setIdUser(UUID.fromString("abcdef01-e89b-12d3-a456-426614174000"));

		// Attempt to add the duplicate song
		boolean addedDuplicate = SongRegistry.addSong(song1Duplicate);

		// Verify the add operation failed
		assertFalse(addedDuplicate, "Adding a song with a duplicate ID should return false.");

		// Verify the registry state hasn't changed
		assertEquals(1, SongRegistry.getAllSongs().size(),
				"Registry size should remain 1 after duplicate add attempt.");
		assertEquals(song1, SongRegistry.getSongById(1), "Original song1 should still be in the registry.");
		assertEquals("Song One", SongRegistry.getSongById(1).getTitle(), "Original song1 title should not change.");
	}

	@Test
	void testAddSong_NullSong() {
		SongRegistry.initialize(new ArrayList<>());
		boolean addedNull = SongRegistry.addSong(null);
		assertFalse(addedNull, "Adding a null song should return false.");
		assertTrue(SongRegistry.getAllSongs().isEmpty(), "Registry should remain empty after attempting to add null.");
	}

	@Test
	void testRemoveSong_NotInitialized() {
		assertFalse(SongRegistry.isInitialized());
		IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
			SongRegistry.removeSong(1);
		}, "Removing a song before initialization should throw IllegalStateException.");
		assertEquals("SongRegistry not initialized.", exception.getMessage());
	}

	@Test
	void testRemoveSong_Found() {
		SongRegistry.initialize(List.of(song1, song2));
		assertEquals(2, SongRegistry.getAllSongs().size());

		Song removed = SongRegistry.removeSong(1);
		assertNotNull(removed, "Removed song should not be null.");
		assertEquals(song1, removed, "Removed song should be song1.");
		assertEquals(1, SongRegistry.getAllSongs().size(), "Registry size should be 1 after removal.");
		assertNull(SongRegistry.getSongById(1), "Song1 should no longer be in the registry.");
		assertNotNull(SongRegistry.getSongById(2), "Song2 should still be in the registry.");
	}

	@Test
	void testRemoveSong_NotFound() {
		SongRegistry.initialize(List.of(song1));
		assertEquals(1, SongRegistry.getAllSongs().size());

		Song removed = SongRegistry.removeSong(99); // Non-existent ID
		assertNull(removed, "Removing a non-existent song should return null.");
		assertEquals(1, SongRegistry.getAllSongs().size(), "Registry size should remain unchanged.");
		assertNotNull(SongRegistry.getSongById(1), "Song1 should still be in the registry.");
	}

	@Test
	void testIsInitialized() {
		assertFalse(SongRegistry.isInitialized(), "Should return false before initialization.");
		SongRegistry.initialize(Collections.emptyList());
		assertTrue(SongRegistry.isInitialized(), "Should return true after initialization.");
		SongRegistry.reset();
		assertFalse(SongRegistry.isInitialized(), "Should return false after reset.");
	}

	@Test
	void testReset() {
		SongRegistry.initialize(List.of(song1, song2));
		assertTrue(SongRegistry.isInitialized());
		assertEquals(2, SongRegistry.getAllSongs().size());

		SongRegistry.reset();

		assertFalse(SongRegistry.isInitialized(), "Registry should not be initialized after reset.");
		// Need to initialize again to check contents
		SongRegistry.initialize(Collections.emptyList());
		assertTrue(SongRegistry.getAllSongs().isEmpty(), "Registry should be empty after reset and re-initialization.");
	}
}
