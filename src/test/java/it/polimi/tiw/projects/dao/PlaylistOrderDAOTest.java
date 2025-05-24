package it.polimi.tiw.projects.dao;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import it.polimi.tiw.projects.exceptions.DAOException;
import it.polimi.tiw.projects.exceptions.DAOException.DAOErrorType;
import it.polimi.tiw.projects.utils.ObjectMapperUtils;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class PlaylistOrderDAOTest {

    private static final Logger log = LoggerFactory.getLogger(PlaylistOrderDAOTest.class);
    private PlaylistOrderDAO playlistOrderDAO;
    private ObjectMapper objectMapper;

    private static final String PLAYLIST_ORDERS_SUBFOLDER = "playlist_orders";
    private static final String JSON_EXTENSION = ".json";

    @TempDir
    Path tempDir;

    private Path playlistOrdersStoragePath;

    @BeforeEach
    void setUp() {
        playlistOrdersStoragePath = tempDir;
        playlistOrderDAO = new PlaylistOrderDAO(playlistOrdersStoragePath);
        objectMapper = ObjectMapperUtils.getMapper();
        log.info("PlaylistOrderDAO initialized with temp directory: {}",
                playlistOrdersStoragePath.resolve(PLAYLIST_ORDERS_SUBFOLDER));
    }

    @AfterEach
    void tearDown() {
        log.info("Test finished. Temp directory {} will be cleaned up.", tempDir);
    }

    private Path getExpectedFilePath(int playlistId) {
        return playlistOrdersStoragePath.resolve(PLAYLIST_ORDERS_SUBFOLDER).resolve(playlistId + JSON_EXTENSION);
    }

    @Test
    @Order(1)
    @DisplayName("Test saving a new playlist order successfully")
    void testSavePlaylistOrder_NewOrder_Success() throws IOException {
        int playlistId = 1;
        List<Integer> songIds = List.of(101, 102, 103);

        assertDoesNotThrow(() -> playlistOrderDAO.savePlaylistOrder(playlistId, songIds),
                "Saving a new playlist order should not throw an exception.");

        Path expectedFile = getExpectedFilePath(playlistId);
        assertTrue(Files.exists(expectedFile), "JSON file for playlist order should be created.");

        // Verify content directly
        List<Integer> savedOrder = objectMapper.readValue(expectedFile.toFile(), new TypeReference<List<Integer>>() {
        });
        assertEquals(songIds, savedOrder, "Saved song IDs should match the input.");
    }

    @Test
    @Order(2)
    @DisplayName("Test updating an existing playlist order")
    void testSavePlaylistOrder_UpdateOrder_Success() throws DAOException, IOException {
        int playlistId = 2;
        List<Integer> initialSongIds = List.of(201, 202);
        playlistOrderDAO.savePlaylistOrder(playlistId, initialSongIds);

        List<Integer> updatedSongIds = List.of(203, 204, 205);
        assertDoesNotThrow(() -> playlistOrderDAO.savePlaylistOrder(playlistId, updatedSongIds),
                "Updating an existing playlist order should not throw an exception.");

        Path expectedFile = getExpectedFilePath(playlistId);
        assertTrue(Files.exists(expectedFile), "JSON file should still exist after update.");

        List<Integer> savedOrder = objectMapper.readValue(expectedFile.toFile(), new TypeReference<List<Integer>>() {
        });
        assertEquals(updatedSongIds, savedOrder, "Saved song IDs should reflect the update.");
    }

    @Test
    @Order(3)
    @DisplayName("Test saving playlist order with empty song list")
    void testSavePlaylistOrder_EmptyList_Success() throws IOException {
        int playlistId = 3;
        List<Integer> emptySongIds = new ArrayList<>();

        assertDoesNotThrow(() -> playlistOrderDAO.savePlaylistOrder(playlistId, emptySongIds),
                "Saving an empty song list should not throw an exception.");

        Path expectedFile = getExpectedFilePath(playlistId);
        assertTrue(Files.exists(expectedFile), "JSON file for empty order should be created.");

        List<Integer> savedOrder = objectMapper.readValue(expectedFile.toFile(), new TypeReference<List<Integer>>() {
        });
        assertTrue(savedOrder.isEmpty(), "Saved order should be an empty list.");
    }

    @Test
    @Order(4)
    @DisplayName("Test saving playlist order with invalid playlist ID (<=0)")
    void testSavePlaylistOrder_InvalidPlaylistId() {
        int invalidPlaylistId = 0;
        List<Integer> songIds = List.of(1, 2);

        DAOException exception = assertThrows(DAOException.class,
                () -> playlistOrderDAO.savePlaylistOrder(invalidPlaylistId, songIds),
                "Saving with invalid playlist ID should throw DAOException.");
        assertEquals(DAOErrorType.GENERIC_ERROR, exception.getErrorType());
        assertTrue(exception.getMessage().contains("Playlist ID must be positive"));
    }

    @Test
    @Order(5)
    @DisplayName("Test saving playlist order with null song list")
    void testSavePlaylistOrder_NullSongList() {
        int playlistId = 4;

        DAOException exception = assertThrows(DAOException.class,
                () -> playlistOrderDAO.savePlaylistOrder(playlistId, null),
                "Saving with null song list should throw DAOException.");
        assertEquals(DAOErrorType.GENERIC_ERROR, exception.getErrorType());
        assertTrue(exception.getMessage().contains("Ordered song IDs list cannot be null"));
    }

    @Test
    @Order(6)
    @DisplayName("Test getting an existing playlist order")
    void testGetPlaylistOrder_Exists_Success() throws DAOException {
        int playlistId = 5;
        List<Integer> expectedSongIds = List.of(501, 502, 503, 504);
        playlistOrderDAO.savePlaylistOrder(playlistId, expectedSongIds);

        List<Integer> actualSongIds = assertDoesNotThrow(() -> playlistOrderDAO.getPlaylistOrder(playlistId),
                "Getting an existing playlist order should not throw an exception.");

        assertNotNull(actualSongIds, "Retrieved song ID list should not be null.");
        assertEquals(expectedSongIds, actualSongIds, "Retrieved song IDs should match the saved ones.");
    }

    @Test
    @Order(7)
    @DisplayName("Test getting playlist order when file does not exist (should return empty list)")
    void testGetPlaylistOrder_NotExists_ReturnsEmptyList() {
        int playlistId = 6;

        List<Integer> actualSongIds = assertDoesNotThrow(() -> playlistOrderDAO.getPlaylistOrder(playlistId),
                "Getting a non-existent playlist order should not throw an exception.");

        assertNotNull(actualSongIds, "Returned list should not be null even if order doesn't exist.");
        assertTrue(actualSongIds.isEmpty(), "Should return an empty list if no custom order file exists.");
    }

    @Test
    @Order(8)
    @DisplayName("Test getting playlist order with invalid playlist ID (<=0)")
    void testGetPlaylistOrder_InvalidPlaylistId() {
        int invalidPlaylistId = -1;

        DAOException exception = assertThrows(DAOException.class,
                () -> playlistOrderDAO.getPlaylistOrder(invalidPlaylistId),
                "Getting with invalid playlist ID should throw DAOException.");
        assertEquals(DAOErrorType.GENERIC_ERROR, exception.getErrorType());
        assertTrue(exception.getMessage().contains("Playlist ID must be positive"));
    }

    @Test
    @Order(9)
    @DisplayName("Test deleting an existing playlist order file")
    void testDeletePlaylistOrder_Exists_Success() throws DAOException {
        int playlistId = 7;
        List<Integer> songIds = List.of(701, 702);
        playlistOrderDAO.savePlaylistOrder(playlistId, songIds);

        Path expectedFile = getExpectedFilePath(playlistId);
        assertTrue(Files.exists(expectedFile), "File should exist before deletion.");

        assertDoesNotThrow(() -> playlistOrderDAO.deletePlaylistOrder(playlistId),
                "Deleting an existing order file should not throw an exception.");

        assertFalse(Files.exists(expectedFile), "File should not exist after deletion.");
    }

    @Test
    @Order(10)
    @DisplayName("Test deleting a non-existent playlist order file (should not throw)")
    void testDeletePlaylistOrder_NotExists_NoException() {
        int playlistId = 8;

        Path expectedFile = getExpectedFilePath(playlistId);
        assertFalse(Files.exists(expectedFile), "File should not exist before attempting deletion.");

        assertDoesNotThrow(() -> playlistOrderDAO.deletePlaylistOrder(playlistId),
                "Deleting a non-existent order file should not throw an exception (deleteIfExists).");

        assertFalse(Files.exists(expectedFile), "File should still not exist after attempting deletion.");
    }

    @Test
    @Order(11)
    @DisplayName("Test deleting playlist order with invalid playlist ID (<=0)")
    void testDeletePlaylistOrder_InvalidPlaylistId() {
        int invalidPlaylistId = 0;

        DAOException exception = assertThrows(DAOException.class,
                () -> playlistOrderDAO.deletePlaylistOrder(invalidPlaylistId),
                "Deleting with invalid playlist ID should throw DAOException.");
        assertEquals(DAOErrorType.GENERIC_ERROR, exception.getErrorType());
        assertTrue(exception.getMessage().contains("Playlist ID must be positive"));
    }

    @Test
    @Order(12)
    @DisplayName("Test save and then get multiple playlist orders")
    void testSaveAndGet_MultipleOrders() throws DAOException {
        int playlistId1 = 10;
        List<Integer> songIds1 = List.of(1001, 1002);
        int playlistId2 = 11;
        List<Integer> songIds2 = List.of(1101, 1102, 1103);
        int playlistId3 = 12;
        List<Integer> songIds3 = new ArrayList<>();

        playlistOrderDAO.savePlaylistOrder(playlistId1, songIds1);
        playlistOrderDAO.savePlaylistOrder(playlistId2, songIds2);
        playlistOrderDAO.savePlaylistOrder(playlistId3, songIds3);

        List<Integer> retrieved1 = playlistOrderDAO.getPlaylistOrder(playlistId1);
        List<Integer> retrieved2 = playlistOrderDAO.getPlaylistOrder(playlistId2);
        List<Integer> retrieved3 = playlistOrderDAO.getPlaylistOrder(playlistId3);

        assertEquals(songIds1, retrieved1, "Order for playlist 10 mismatch.");
        assertEquals(songIds2, retrieved2, "Order for playlist 11 mismatch.");
        assertEquals(songIds3, retrieved3, "Order for playlist 12 (empty) mismatch.");
        assertTrue(retrieved3.isEmpty(), "Retrieved order for playlist 12 should be empty.");
    }
}
