package it.polimi.tiw.projects.utils;

import it.polimi.tiw.projects.beans.Playlist;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class PlaylistRegistryTest {

    private List<Playlist> initialPlaylists;

    @BeforeEach
    void setUp() {
        // Create some sample playlists for testing
        initialPlaylists = new ArrayList<>();
        Playlist playlist1 = new Playlist();
        playlist1.setIdPlaylist(1);
        playlist1.setName("My Playlist 1");
        playlist1.setBirthday(new Timestamp(System.currentTimeMillis()));
        playlist1.setIdUser(UUID.randomUUID());
        playlist1.setSongs(List.of(1, 2, 3));

        Playlist playlist2 = new Playlist();
        playlist2.setIdPlaylist(2);
        playlist2.setName("My Playlist 2");
        playlist2.setBirthday(new Timestamp(System.currentTimeMillis()));
        playlist2.setIdUser(UUID.randomUUID());
        playlist2.setSongs(List.of(4, 5, 6));

        initialPlaylists.add(playlist1);
        initialPlaylists.add(playlist2);

        // Initialize the registry with the sample playlists
        PlaylistRegistry.initialize(initialPlaylists);
    }

    @AfterEach
    void tearDown() {
        // Reset the registry after each test
        PlaylistRegistry.reset();
    }

    @Test
    void initialize_validInput_registryInitialized() {
        assertTrue(PlaylistRegistry.isInitialized());
        Collection<Playlist> playlists = PlaylistRegistry.getAllPlaylists();
        assertEquals(initialPlaylists.size(), playlists.size());
    }

    @Test
    void initialize_alreadyInitialized_throwsException() {
        assertThrows(IllegalStateException.class, () -> PlaylistRegistry.initialize(initialPlaylists));
    }

    @Test
    void getPlaylistById_existingId_returnsPlaylist() {
        Playlist playlist = PlaylistRegistry.getPlaylistById(1);
        assertNotNull(playlist);
        assertEquals("My Playlist 1", playlist.getName());
    }

    @Test
    void getPlaylistById_nonExistingId_returnsNull() {
        Playlist playlist = PlaylistRegistry.getPlaylistById(99);
        assertNull(playlist);
    }

    @Test
    void getAllPlaylists_registryInitialized_returnsAllPlaylists() {
        Collection<Playlist> playlists = PlaylistRegistry.getAllPlaylists();
        assertNotNull(playlists);
        assertEquals(initialPlaylists.size(), playlists.size());
    }

    @Test
    void addPlaylist_validPlaylist_returnsTrue() {
        Playlist newPlaylist = new Playlist();
        newPlaylist.setIdPlaylist(3);
        newPlaylist.setName("My Playlist 3");
        newPlaylist.setBirthday(new Timestamp(System.currentTimeMillis()));
        newPlaylist.setIdUser(UUID.randomUUID());
        newPlaylist.setSongs(List.of(7, 8, 9));

        boolean added = PlaylistRegistry.addPlaylist(newPlaylist);
        assertTrue(added);
        assertEquals(3, PlaylistRegistry.getAllPlaylists().size());
    }

    @Test
    void addPlaylist_nullPlaylist_returnsFalse() {
        boolean added = PlaylistRegistry.addPlaylist(null);
        assertFalse(added);
        assertEquals(2, PlaylistRegistry.getAllPlaylists().size());
    }

    @Test
    void addPlaylist_duplicateId_returnsFalse() {
        Playlist newPlaylist = new Playlist();
        newPlaylist.setIdPlaylist(1); // Same ID as existing playlist
        newPlaylist.setName("My Playlist 3");
        newPlaylist.setBirthday(new Timestamp(System.currentTimeMillis()));
        newPlaylist.setIdUser(UUID.randomUUID());
        newPlaylist.setSongs(List.of(7, 8, 9));

        boolean added = PlaylistRegistry.addPlaylist(newPlaylist);
        assertFalse(added);
        assertEquals(2, PlaylistRegistry.getAllPlaylists().size());
    }

    @Test
    void removePlaylist_existingId_returnsPlaylist() {
        Playlist removedPlaylist = PlaylistRegistry.removePlaylist(1);
        assertNotNull(removedPlaylist);
        assertEquals("My Playlist 1", removedPlaylist.getName());
        assertEquals(1, PlaylistRegistry.getAllPlaylists().size());
    }

    @Test
    void removePlaylist_nonExistingId_returnsNull() {
        Playlist removedPlaylist = PlaylistRegistry.removePlaylist(99);
        assertNull(removedPlaylist);
        assertEquals(2, PlaylistRegistry.getAllPlaylists().size());
    }

    @Test
    void reset_registryInitialized_registryReset() {
        PlaylistRegistry.reset();
        assertFalse(PlaylistRegistry.isInitialized());
    }

    @Test
    void getPlaylistById_notInitialized_throwsException() {
        PlaylistRegistry.reset();
        assertThrows(IllegalStateException.class, () -> PlaylistRegistry.getPlaylistById(1));
    }

    @Test
    void getAllPlaylists_notInitialized_throwsException() {
        PlaylistRegistry.reset();
        assertThrows(IllegalStateException.class, () -> PlaylistRegistry.getAllPlaylists());
    }

    @Test
    void addPlaylist_notInitialized_throwsException() {
        PlaylistRegistry.reset();
        Playlist newPlaylist = new Playlist();
        newPlaylist.setIdPlaylist(3);
        newPlaylist.setName("My Playlist 3");
        newPlaylist.setBirthday(new Timestamp(System.currentTimeMillis()));
        newPlaylist.setIdUser(UUID.randomUUID());
        newPlaylist.setSongs(List.of(7, 8, 9));
        assertThrows(IllegalStateException.class, () -> PlaylistRegistry.addPlaylist(newPlaylist));
    }

    @Test
    void removePlaylist_notInitialized_throwsException() {
        PlaylistRegistry.reset();
        assertThrows(IllegalStateException.class, () -> PlaylistRegistry.removePlaylist(1));
    }
}
