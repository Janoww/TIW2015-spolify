package it.polimi.tiw.projects.controllers;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.polimi.tiw.projects.beans.Album;
import it.polimi.tiw.projects.beans.Playlist;
import it.polimi.tiw.projects.beans.Song;
import it.polimi.tiw.projects.beans.SongWithAlbum;
import it.polimi.tiw.projects.beans.User;
import it.polimi.tiw.projects.dao.AlbumDAO;
import it.polimi.tiw.projects.dao.PlaylistDAO;
import it.polimi.tiw.projects.dao.SongDAO;
import it.polimi.tiw.projects.exceptions.DAOException;
import it.polimi.tiw.projects.utils.ConnectionHandler;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet("/GetPlaylistDetails")
public class GetPlaylistDetails extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private static final Logger logger = LoggerFactory.getLogger(GetPlaylistDetails.class);
	private Connection connection;

	public GetPlaylistDetails() {
		super();
	}

	@Override
	public void init() throws ServletException {
		ServletContext context = getServletContext();
		connection = ConnectionHandler.getConnection(context);
		logger.info("GetPlaylistDetails servlet initialized.");
	}

	@Override
	public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		logger.debug("Received GET request for playlist details.");
		PlaylistDAO playlistDAO = new PlaylistDAO(connection);
		SongDAO songDAO = new SongDAO(connection);
		AlbumDAO albumDAO = new AlbumDAO(connection);
		User user = (User) req.getSession().getAttribute("user");

		if (user == null || req.getSession().isNew()) {
			logger.warn("User not authenticated or session is new. Denying access.");
			resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
			resp.setContentType("application/json");
			resp.setCharacterEncoding("UTF-8");
			ObjectMapper mapper = new ObjectMapper();
			Map<String, String> errorResponse = new HashMap<>();
			errorResponse.put("error", "User not authenticated");
			resp.getWriter().write(mapper.writeValueAsString(errorResponse));
			return;
		}
		UUID userId = user.getIdUser();
		logger.debug("User {} (ID: {}) authenticated.", user.getUsername(), userId);

		Integer playlistId;
		String playlistIdParam = req.getParameter("playlistId");
		try {
			if (playlistIdParam == null || playlistIdParam.strip().isEmpty()) {
				logger.warn("playlistId parameter is missing for user ID: {}", userId);
				throw new NullPointerException("playlistId parameter is missing");
			}
			playlistId = Integer.parseInt(playlistIdParam.strip());
			logger.debug("Requested playlist ID: {} by user ID: {}", playlistId, userId);
		} catch (NumberFormatException | NullPointerException e) {
			logger.warn(
					"Invalid or missing playlistId parameter: '{}' for user ID: {}. Details: {}",
					playlistIdParam, userId, e.getMessage());
			resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			resp.setContentType("application/json");
			resp.setCharacterEncoding("UTF-8");
			ObjectMapper mapper = new ObjectMapper();
			Map<String, String> errorResponse = new HashMap<>();
			errorResponse.put("error", "Invalid or missing playlistId parameter");
			resp.getWriter().write(mapper.writeValueAsString(errorResponse));
			return;
		}

		Integer page = 0;
		String pageParam = req.getParameter("page");
		try {
			if (pageParam != null && !pageParam.strip().isEmpty()) {
				page = Integer.parseInt(pageParam.strip());
			}
			if (page < 0) {
				logger.debug(
						"Requested page {} is negative, defaulting to 0. Playlist ID: {}, User ID: {}",
						page, playlistId, userId);
				page = 0;
			}
		} catch (NumberFormatException e) {
			logger.warn(
					"Invalid page parameter: '{}'. Defaulting to page 0. Playlist ID: {}, User ID: {}",
					pageParam, playlistId, userId);
			page = 0; // Default to 0 if parsing fails or param is invalid
		}
		logger.debug("Requested page: {}. Playlist ID: {}, User ID: {}", page, playlistId, userId);

		Playlist myPlaylist;
		try {
			myPlaylist = playlistDAO.findPlaylistById(playlistId, userId);
			logger.debug("Successfully fetched playlist ID: {} for user ID: {}", playlistId,
					userId);
		} catch (DAOException e) {
			String errorMessage = "Error accessing playlist";
			int statusCode = HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
			// Specific logging for expected/unexpected errors is handled in the switch
			// cases.
			switch (e.getErrorType()) {
				case NOT_FOUND:
				case ACCESS_DENIED:
					errorMessage = "Playlist not found";
					statusCode = HttpServletResponse.SC_NOT_FOUND;
					logger.warn("Playlist ID: {} not found or access denied for user ID: {}",
							playlistId, userId);
					break;
				default:
					logger.error("Unhandled DAOException type: {}. Details: {}", e.getErrorType(),
							e.getMessage(), e);
					break;
			}
			resp.setStatus(statusCode);
			resp.setContentType("application/json");
			resp.setCharacterEncoding("UTF-8");
			ObjectMapper mapper = new ObjectMapper();
			Map<String, String> errorResponse = new HashMap<>();
			errorResponse.put("error", errorMessage);
			resp.getWriter().write(mapper.writeValueAsString(errorResponse));
			return;
		}

		List<Song> allPlaylistSongsOrdered = orderAllSongs(myPlaylist, songDAO, albumDAO);
		if (allPlaylistSongsOrdered == null) {
			logger.debug(
					"No songs found or error in ordering for playlist ID: {}. Initializing to empty list.",
					playlistId);
			allPlaylistSongsOrdered = new ArrayList<>();
		}
		logger.debug("Total songs in playlist ID {}: {}. Ordered: {}", playlistId,
				myPlaylist.getSongs().size(), allPlaylistSongsOrdered.size());

		int totPages = (allPlaylistSongsOrdered.size() + 4) / 5; // 5 songs per page
		if (totPages == 0) {
			page = 0;
			logger.debug("No songs in playlist ID: {}, so total pages is 0, current page is 0.",
					playlistId);
		} else if (page >= totPages) {
			page = totPages - 1;
			logger.debug(
					"Requested page {} is out of bounds (total pages: {}). Setting to last page: {}. Playlist ID: {}",
					req.getParameter("page"), totPages, page, playlistId);
		}
		logger.debug("Pagination for playlist ID {}: Current Page: {}, Total Pages: {}", playlistId,
				page, totPages);

		List<Song> songsOnPage = allPlaylistSongsOrdered.stream().skip(page * 5L).limit(5)
				.collect(Collectors.toList());
		logger.debug("Songs on current page ({}) for playlist ID {}: {} songs.", page, playlistId,
				songsOnPage.size());

		List<SongWithAlbum> songsWithAlbumList = songsOnPage.stream().map(s -> {
			try {
				Album album = albumDAO.findAlbumById(s.getIdAlbum());
				logger.trace("Fetched album ID: {} for song ID: {}", s.getIdAlbum(), s.getIdSong());
				return new SongWithAlbum(s, album);
			} catch (DAOException e) {
				logger.error(
						"DAOException while fetching album ID: {} for song ID: {}. ErrorType: {}",
						s.getIdAlbum(), s.getIdSong(), e.getErrorType(), e);
				return new SongWithAlbum(s, null); // Song without album info if album fetch fails
			}
		}).collect(Collectors.toList());
		logger.debug("Mapped {} songs on page to SongWithAlbum objects for playlist ID: {}",
				songsWithAlbumList.size(), playlistId);

		List<Song> unusedSongs = getUnusedSongs(myPlaylist, songDAO);
		if (unusedSongs == null) {
			logger.debug(
					"No unused songs found or error in fetching for playlist ID: {}. Initializing to empty list.",
					playlistId);
			unusedSongs = new ArrayList<>();
		}
		logger.debug("Found {} unused songs for user ID: {}", unusedSongs.size(), userId);

		resp.setStatus(HttpServletResponse.SC_OK);
		resp.setContentType("application/json");
		resp.setCharacterEncoding("UTF-8");

		ObjectMapper mapper = new ObjectMapper();
		Map<String, Object> responseData = new HashMap<>();
		responseData.put("playlist", myPlaylist);
		responseData.put("songsPage", songsWithAlbumList);
		responseData.put("currentPage", page);
		responseData.put("totalPages", totPages);
		responseData.put("unusedSongs", unusedSongs);

		resp.getWriter().write(mapper.writeValueAsString(responseData));
		logger.debug("Successfully sent OK response with playlist details for playlist ID: {}",
				playlistId);
	}

	@Override
	public void destroy() {
		try {
			ConnectionHandler.closeConnection(connection);
			logger.info("GetPlaylistDetails servlet destroyed. Connection closed.");
		} catch (SQLException e) {
			logger.error("Failed to close database connection on servlet destroy.", e);
		}
	}

	private static List<Song> orderAllSongs(Playlist playlist, SongDAO songDao, AlbumDAO albumDao) {
		logger.debug("Ordering songs for playlist ID: {}", playlist.getIdPlaylist());
		List<Song> result = null;
		// TODO: Implement actual song ordering logic.
		// For now, returning songs as is or an empty list if null.
		List<Integer> songsIDs = playlist.getSongs();
		if (songsIDs != null) {
			result = new ArrayList<>();
			UUID playlistUserId = playlist.getIdUser(); // Get user ID from playlist
			for (Integer songId : songsIDs) {
				try {
					// Use findSongsByIdsAndUser, expecting a single song or empty list
					List<Song> songsFound =
							songDao.findSongsByIdsAndUser(List.of(songId), playlistUserId);
					if (songsFound != null && !songsFound.isEmpty()) {
						Song song = songsFound.get(0); // Get the first (and should be only) song
						result.add(song);
					} else {
						// This case means songId was not found OR it doesn't belong to
						// playlistUserId
						logger.warn(
								"Song ID: {} (for playlist ID: {}) not found or not accessible for user ID: {}.",
								songId, playlist.getIdPlaylist(), playlistUserId);
					}
				} catch (DAOException e) {
					logger.error(
							"DAOException while fetching song ID: {} for playlist ID: {} (User ID: {}). ErrorType: {}",
							songId, playlist.getIdPlaylist(), playlistUserId, e.getErrorType(), e);
				}
			}
			// Placeholder for actual ordering logic if needed
			logger.debug("Retrieved {} songs for playlist ID: {}. Actual ordering logic TBD.",
					result.size(), playlist.getIdPlaylist());
		} else {
			logger.debug("Playlist ID: {} has no song IDs associated.", playlist.getIdPlaylist());
			result = new ArrayList<>();
		}
		return result;
	}

	private static List<Song> getUnusedSongs(Playlist playlist, SongDAO songDao) {
		logger.debug("Getting unused songs for user ID: {} (related to playlist ID: {})",
				playlist.getIdUser(), playlist.getIdPlaylist());
		// TODO: Implement actual logic to find songs by user not in this playlist.
		// For now, returning null, which will be handled as an empty list.
		List<Song> allUserSongs;
		List<Song> unusedSongs = new ArrayList<>();
		try {
			allUserSongs = songDao.findSongsByUser(playlist.getIdUser());
			if (allUserSongs != null) {
				List<Integer> playlistSongIds =
						playlist.getSongs() != null ? playlist.getSongs() : new ArrayList<>();
				for (Song song : allUserSongs) {
					if (!playlistSongIds.contains(song.getIdSong())) {
						unusedSongs.add(song);
					}
				}
				logger.debug(
						"Found {} unused songs for user ID: {} (playlist ID: {}). Total user songs: {}",
						unusedSongs.size(), playlist.getIdUser(), playlist.getIdPlaylist(),
						allUserSongs.size());
			} else {
				logger.debug("User ID: {} has no songs. Playlist ID: {}", playlist.getIdUser(),
						playlist.getIdPlaylist());
			}
		} catch (DAOException e) {
			logger.error(
					"DAOException while fetching all songs for user ID: {} (for unused songs logic, playlist ID: {}). ErrorType: {}",
					playlist.getIdUser(), playlist.getIdPlaylist(), e.getErrorType(), e);
			return new ArrayList<>(); // Return empty list on error
		}
		return unusedSongs;
	}

}
