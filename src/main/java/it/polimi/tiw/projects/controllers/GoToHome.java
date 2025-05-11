package it.polimi.tiw.projects.controllers;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.polimi.tiw.projects.beans.Playlist;
import it.polimi.tiw.projects.beans.Song;
import it.polimi.tiw.projects.beans.User;
import it.polimi.tiw.projects.dao.PlaylistDAO;
import it.polimi.tiw.projects.dao.SongDAO;
import it.polimi.tiw.projects.exceptions.DAOException;
import it.polimi.tiw.projects.utils.ConnectionHandler;
import it.polimi.tiw.projects.utils.Genre;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class GoToHome extends HttpServlet {
	static final long serialVersionUID = 1L;
	private static final Logger logger = LoggerFactory.getLogger(GoToHome.class);
	private Connection connection;

	public GoToHome() {
		super();
	}

	@Override
	public void init() throws ServletException {
		ServletContext context = getServletContext();
		connection = ConnectionHandler.getConnection(context);
		logger.info("GoToHome servlet initialized.");
	}

	@Override
	public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		logger.debug("Received GET request for home page data.");
		PlaylistDAO playlistDAO = new PlaylistDAO(connection);
		SongDAO songDAO = new SongDAO(connection);
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
		logger.debug("User {} (ID: {}) authenticated. Fetching home page data.", user.getUsername(), userId);

		List<Integer> playlistIDs;
		List<Song> songList;
		List<Genre> genresList = Arrays.asList(Genre.values());
		logger.debug("Available genres: {}", genresList);

		try {
			playlistIDs = playlistDAO.findPlaylistIdsByUser(userId);
			logger.debug("Found {} playlist IDs for user ID: {}", playlistIDs.size(), userId);
			songList = songDAO.findSongsByUser(userId);
			logger.debug("Found {} songs for user ID: {}", songList.size(), userId);
		} catch (DAOException e) {
			logger.error("DAOException while fetching initial data for user ID: {}. ErrorType: {}", userId,
					e.getErrorType(), e);
			resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			resp.setContentType("application/json");
			resp.setCharacterEncoding("UTF-8");
			ObjectMapper mapper = new ObjectMapper();
			Map<String, String> errorResponse = new HashMap<>();
			errorResponse.put("error", "Error fetching data from database");
			resp.getWriter().write(mapper.writeValueAsString(errorResponse));
			return;
		}

		List<Playlist> playlists = playlistIDs.stream().map(id -> {
			try {
				logger.trace("Fetching playlist details for ID: {} by user ID: {}", id, userId);
				return playlistDAO.findPlaylistById(id, userId);
			} catch (DAOException e) {
				logger.error("DAOException while fetching playlist details for ID: {} by user ID: {}. ErrorType: {}",
						id, userId, e.getErrorType(), e);
				return null; // Allow other playlists to be fetched
			}
		}).filter(p -> p != null) // Filter out playlists that failed to load
				.collect(java.util.stream.Collectors.toList());
		logger.debug("Successfully fetched details for {} playlists for user ID: {}", playlists.size(), userId);

		resp.setStatus(HttpServletResponse.SC_OK);
		resp.setContentType("application/json");
		resp.setCharacterEncoding("UTF-8");

		ObjectMapper mapper = new ObjectMapper();
		Map<String, Object> responseData = new HashMap<>();
		responseData.put("playlists", playlists);
		responseData.put("songs", songList);
		responseData.put("genres", genresList);

		resp.getWriter().write(mapper.writeValueAsString(responseData));
		logger.debug("Successfully sent OK response with home page data for user ID: {}", userId);
	}

	@Override
	public void destroy() {
		try {
			ConnectionHandler.closeConnection(connection);
			logger.info("GoToHome servlet destroyed. Connection closed.");
		} catch (SQLException e) {
			logger.error("Failed to close database connection on servlet destroy.", e);
		}
	}
}
