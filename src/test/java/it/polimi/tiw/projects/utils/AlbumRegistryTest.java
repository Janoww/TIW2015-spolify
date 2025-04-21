package it.polimi.tiw.projects.utils;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import it.polimi.tiw.projects.beans.Album;

public class AlbumRegistryTest {

	private Album album1;
	private Album album2;

	@BeforeEach
	void setUp() {
		// Reset the registry before each test to ensure isolation
		AlbumRegistry.reset();

		album1 = new Album();
		album1.setIdAlbum(1);
		album1.setName("Album One");
		album1.setArtist("Artist One");
		album1.setYear(2023);

		album2 = new Album();
		album2.setIdAlbum(2);
		album2.setName("Album Two");
		album2.setArtist("Artist Two");
		album2.setYear(2024);
	}

	@Test
	void testInitialize_Success() {
		assertFalse(AlbumRegistry.isInitialized(), "Registry should not be initialized initially.");
		List<Album> initialAlbums = new ArrayList<>();
		initialAlbums.add(album1);
		initialAlbums.add(album2);

		AlbumRegistry.initialize(initialAlbums);

		assertTrue(AlbumRegistry.isInitialized(), "Registry should be initialized after calling initialize.");
		assertEquals(2, AlbumRegistry.getAllAlbums().size(), "Registry should contain 2 albums after initialization.");
		assertEquals(album1, AlbumRegistry.getAlbumById(1), "Should retrieve album1 by ID.");
		assertEquals(album2, AlbumRegistry.getAlbumById(2), "Should retrieve album2 by ID.");
	}

	@Test
	void testInitialize_NullList() {
		assertFalse(AlbumRegistry.isInitialized());
		AlbumRegistry.initialize(null);
		assertTrue(AlbumRegistry.isInitialized());
		assertTrue(AlbumRegistry.getAllAlbums().isEmpty(), "Registry should be empty when initialized with null.");
	}

	@Test
	void testInitialize_EmptyList() {
		assertFalse(AlbumRegistry.isInitialized());
		AlbumRegistry.initialize(new ArrayList<>());
		assertTrue(AlbumRegistry.isInitialized());
		assertTrue(AlbumRegistry.getAllAlbums().isEmpty(),
				"Registry should be empty when initialized with an empty list.");
	}

	@Test
	void testInitialize_AlreadyInitialized() {
		AlbumRegistry.initialize(Collections.emptyList()); // Initialize once
		assertTrue(AlbumRegistry.isInitialized());

		List<Album> secondInitializationList = new ArrayList<>();
		secondInitializationList.add(album1);

		// Attempt to initialize again
		IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
			AlbumRegistry.initialize(secondInitializationList);
		}, "Initializing an already initialized registry should throw IllegalStateException.");

		assertEquals("AlbumRegistry already initialized.", exception.getMessage());
		assertTrue(AlbumRegistry.getAllAlbums().isEmpty(),
				"Registry content should not change on re-initialization attempt.");
	}

	@Test
	void testGetAlbumById_NotInitialized() {
		assertFalse(AlbumRegistry.isInitialized());
		IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
			AlbumRegistry.getAlbumById(1);
		}, "Getting an album before initialization should throw IllegalStateException.");
		assertEquals("AlbumRegistry not initialized.", exception.getMessage());
	}

	@Test
	void testGetAlbumById_Found() {
		List<Album> initialAlbums = new ArrayList<>();
		initialAlbums.add(album1);
		AlbumRegistry.initialize(initialAlbums);

		Album foundAlbum = AlbumRegistry.getAlbumById(1);
		assertNotNull(foundAlbum, "Should find album1.");
		assertEquals(album1, foundAlbum, "Found album should be equal to album1.");
	}

	@Test
	void testGetAlbumById_NotFound() {
		List<Album> initialAlbums = new ArrayList<>();
		initialAlbums.add(album1);
		AlbumRegistry.initialize(initialAlbums);

		Album foundAlbum = AlbumRegistry.getAlbumById(99); // Non-existent ID
		assertNull(foundAlbum, "Should return null for a non-existent album ID.");
	}

	@Test
	void testGetAllAlbums_NotInitialized() {
		assertFalse(AlbumRegistry.isInitialized());
		IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
			AlbumRegistry.getAllAlbums();
		}, "Getting all albums before initialization should throw IllegalStateException.");
		assertEquals("AlbumRegistry not initialized.", exception.getMessage());
	}

	@Test
	void testGetAllAlbums_Success() {
		List<Album> initialAlbums = new ArrayList<>();
		initialAlbums.add(album1);
		initialAlbums.add(album2);
		AlbumRegistry.initialize(initialAlbums);

		Collection<Album> allAlbums = AlbumRegistry.getAllAlbums();
		assertNotNull(allAlbums, "Returned collection should not be null.");
		assertEquals(2, allAlbums.size(), "Returned collection should contain 2 albums.");
		assertTrue(allAlbums.contains(album1), "Collection should contain album1.");
		assertTrue(allAlbums.contains(album2), "Collection should contain album2.");
	}

	@Test
	void testGetAllAlbums_IsUnmodifiable() {
		List<Album> initialAlbums = new ArrayList<>();
		initialAlbums.add(album1);
		AlbumRegistry.initialize(initialAlbums);

		Collection<Album> allAlbums = AlbumRegistry.getAllAlbums();

		// Attempt to modify the returned collection
		Executable addAttempt = () -> allAlbums.add(album2); // Create a new album instance if needed
		Executable removeAttempt = () -> allAlbums.remove(album1);
		Executable clearAttempt = allAlbums::clear;

		assertThrows(UnsupportedOperationException.class, addAttempt,
				"Should not be able to add to the returned collection.");
		assertThrows(UnsupportedOperationException.class, removeAttempt,
				"Should not be able to remove from the returned collection.");
		assertThrows(UnsupportedOperationException.class, clearAttempt,
				"Should not be able to clear the returned collection.");
	}

	@Test
	void testAddAlbum_NotInitialized() {
		assertFalse(AlbumRegistry.isInitialized());
		IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
			AlbumRegistry.addAlbum(album1);
		}, "Adding an album before initialization should throw IllegalStateException.");
		assertEquals("AlbumRegistry not initialized.", exception.getMessage());
	}

	@Test
	void testAddAlbum_Success() {
		AlbumRegistry.initialize(new ArrayList<>()); // Initialize empty
		assertTrue(AlbumRegistry.getAllAlbums().isEmpty());

		boolean added1 = AlbumRegistry.addAlbum(album1);
		assertTrue(added1, "addAlbum should return true for the first album.");
		assertEquals(1, AlbumRegistry.getAllAlbums().size(), "Registry should have 1 album after adding.");
		assertEquals(album1, AlbumRegistry.getAlbumById(1), "Should retrieve the added album.");

		boolean added2 = AlbumRegistry.addAlbum(album2);
		assertTrue(added2, "addAlbum should return true for the second album.");
		assertEquals(2, AlbumRegistry.getAllAlbums().size(), "Registry should have 2 albums after adding another.");
		assertEquals(album2, AlbumRegistry.getAlbumById(2), "Should retrieve the second added album.");
	}

	@Test
	void testAddAlbum_DuplicateId() {
		AlbumRegistry.initialize(List.of(album1)); // Initialize with album1
		assertEquals(1, AlbumRegistry.getAllAlbums().size());

		// Create another album with the same ID
		Album album1Duplicate = new Album();
		album1Duplicate.setIdAlbum(1); // Same ID as album1
		album1Duplicate.setName("Album One Duplicate");
		album1Duplicate.setArtist("Artist One");
		album1Duplicate.setYear(2025);

		// Attempt to add the duplicate album
		boolean addedDuplicate = AlbumRegistry.addAlbum(album1Duplicate);

		// Verify the add operation failed
		assertFalse(addedDuplicate, "Adding an album with a duplicate ID should return false.");

		// Verify the registry state hasn't changed
		assertEquals(1, AlbumRegistry.getAllAlbums().size(),
				"Registry size should remain 1 after duplicate add attempt.");
		assertEquals(album1, AlbumRegistry.getAlbumById(1), "Original album1 should still be in the registry.");
		assertEquals("Album One", AlbumRegistry.getAlbumById(1).getName(), "Original album1 name should not change.");
	}

	@Test
	void testAddAlbum_NullAlbum() {
		AlbumRegistry.initialize(new ArrayList<>());
		boolean addedNull = AlbumRegistry.addAlbum(null);
		assertFalse(addedNull, "Adding a null album should return false.");
		assertTrue(AlbumRegistry.getAllAlbums().isEmpty(),
				"Registry should remain empty after attempting to add null.");
	}

	@Test
	void testRemoveAlbum_NotInitialized() {
		assertFalse(AlbumRegistry.isInitialized());
		IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
			AlbumRegistry.removeAlbum(1);
		}, "Removing an album before initialization should throw IllegalStateException.");
		assertEquals("AlbumRegistry not initialized.", exception.getMessage());
	}

	@Test
	void testRemoveAlbum_Found() {
		AlbumRegistry.initialize(List.of(album1, album2));
		assertEquals(2, AlbumRegistry.getAllAlbums().size());

		Album removed = AlbumRegistry.removeAlbum(1);
		assertNotNull(removed, "Removed album should not be null.");
		assertEquals(album1, removed, "Removed album should be album1.");
		assertEquals(1, AlbumRegistry.getAllAlbums().size(), "Registry size should be 1 after removal.");
		assertNull(AlbumRegistry.getAlbumById(1), "Album1 should no longer be in the registry.");
		assertNotNull(AlbumRegistry.getAlbumById(2), "Album2 should still be in the registry.");
	}

	@Test
	void testRemoveAlbum_NotFound() {
		AlbumRegistry.initialize(List.of(album1));
		assertEquals(1, AlbumRegistry.getAllAlbums().size());

		Album removed = AlbumRegistry.removeAlbum(99); // Non-existent ID
		assertNull(removed, "Removing a non-existent album should return null.");
		assertEquals(1, AlbumRegistry.getAllAlbums().size(), "Registry size should remain unchanged.");
		assertNotNull(AlbumRegistry.getAlbumById(1), "Album1 should still be in the registry.");
	}

	@Test
	void testIsInitialized() {
		assertFalse(AlbumRegistry.isInitialized(), "Should return false before initialization.");
		AlbumRegistry.initialize(Collections.emptyList());
		assertTrue(AlbumRegistry.isInitialized(), "Should return true after initialization.");
		AlbumRegistry.reset();
		assertFalse(AlbumRegistry.isInitialized(), "Should return false after reset.");
	}

	@Test
	void testReset() {
		AlbumRegistry.initialize(List.of(album1, album2));
		assertTrue(AlbumRegistry.isInitialized());
		assertEquals(2, AlbumRegistry.getAllAlbums().size());

		AlbumRegistry.reset();

		assertFalse(AlbumRegistry.isInitialized(), "Registry should not be initialized after reset.");
		// Need to initialize again to check contents
		AlbumRegistry.initialize(Collections.emptyList());
		assertTrue(AlbumRegistry.getAllAlbums().isEmpty(),
				"Registry should be empty after reset and re-initialization.");
	}
}
