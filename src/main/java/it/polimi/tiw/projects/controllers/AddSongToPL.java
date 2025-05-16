package it.polimi.tiw.projects.controllers;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.polimi.tiw.projects.beans.User;
import it.polimi.tiw.projects.dao.PlaylistDAO;
import it.polimi.tiw.projects.exceptions.DAOException;
import it.polimi.tiw.projects.utils.ConnectionHandler;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet("/AddSongToPL")
public class AddSongToPL extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private static final Logger logger = LoggerFactory.getLogger(AddSongToPL.class);
	private Connection connection;

	public AddSongToPL() {
		super();
	}

	@Override
	public void init() throws ServletException {
		ServletContext context = getServletContext();
		connection = ConnectionHandler.getConnection(context);
		logger.info("AddSongToPL servlet initialized.");
	}

	@Override
	public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		logger.debug("Received POST request to add song(s) to playlist.");
		PlaylistDAO playlistDAO = new PlaylistDAO(connection);
		User user = (User) req.getSession().getAttribute("user");

		if (user == null) {
			logger.warn("User not authenticated. Denying access.");
			resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
			resp.setContentType("application/json");
			resp.setCharacterEncoding("UTF-8");
			ObjectMapper mapper = new ObjectMapper();
			Map<String, String> errorResponse = new HashMap<>();
			errorResponse.put("error", "User not authenticated");
			resp.getWriter().write(mapper.writeValueAsString(errorResponse));
			return;
		}
		logger.debug("User {} authenticated.", user.getUsername());

		String checkResult = areParametersOk(req);
		if (checkResult != null) {
			logger.warn("Parameter validation failed: {}", checkResult);
			resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			resp.setContentType("application/json");
			resp.setCharacterEncoding("UTF-8");
			ObjectMapper mapper = new ObjectMapper();
			Map<String, String> errorResponse = new HashMap<>();
			errorResponse.put("error", checkResult);
			resp.getWriter().write(mapper.writeValueAsString(errorResponse));
			return;
		}
		logger.debug("Parameters validated successfully.");

		List<Integer> songIDs = Arrays.stream(req.getParameterValues("songsSelect"))
				.map(Integer::parseInt).collect(Collectors.toList());
		Integer playlistId = Integer.parseInt(req.getParameter("playlistId").strip());
		logger.debug("Attempting to add songs {} to playlist ID: {} for user ID: {}", songIDs,
				playlistId, user.getIdUser());

		try {
			for (Integer songId : songIDs) {
				logger.debug("Adding song ID: {} to playlist ID: {}", songId, playlistId);
				playlistDAO.addSongToPlaylist(playlistId, user.getIdUser(), songId);
			}
			logger.info("Successfully added {} song(s) to playlist ID: {} for user ID: {}",
					songIDs.size(), playlistId, user.getIdUser());
		} catch (DAOException e) {
			String errorMessage = "Error adding song(s) to playlist";
			int statusCode = HttpServletResponse.SC_INTERNAL_SERVER_ERROR;

			switch (e.getErrorType()) {
				case NOT_FOUND:
					errorMessage = e.getMessage(); // Specific message from DAO
					statusCode = HttpServletResponse.SC_NOT_FOUND;
					logger.warn("Playlist or song not found: {}", errorMessage);
					break;
				case ACCESS_DENIED:
					errorMessage = "Playlist not found or access denied"; // Generic for security
					statusCode = HttpServletResponse.SC_NOT_FOUND; // Or SC_FORBIDDEN
					logger.warn("Access denied for playlist ID: {}", playlistId);
					break;
				case DUPLICATE_ENTRY:
					errorMessage = e.getMessage(); // Specific message from DAO
					statusCode = HttpServletResponse.SC_CONFLICT;
					logger.warn("Duplicate entry detected: {}", errorMessage);
					break;
				default:
					logger.error("Unhandled DAOException type: {}. Details: {}", e.getErrorType(),
							e.getMessage(), e);
					// errorMessage and statusCode remain as defaults
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

		resp.setStatus(HttpServletResponse.SC_OK);
		resp.setContentType("application/json");
		resp.setCharacterEncoding("UTF-8");
		ObjectMapper mapper = new ObjectMapper();
		Map<String, String> successResponse = new HashMap<>();
		successResponse.put("message", "Song(s) added successfully to playlist");
		resp.getWriter().write(mapper.writeValueAsString(successResponse));
		logger.debug("Successfully sent OK response for adding songs to playlist.");
	}

	@Override
	public void destroy() {
		try {
			ConnectionHandler.closeConnection(connection);
			logger.info("AddSongToPL servlet destroyed. Connection closed.");
		} catch (SQLException e) {
			logger.error("Failed to close database connection on servlet destroy.", e);
		}
	}

	private String areParametersOk(HttpServletRequest req) {
		logger.debug("Validating parameters for adding song to playlist.");
		String playlistString = req.getParameter("playlistId");
		if (playlistString == null || (playlistString = playlistString.strip()).isEmpty()) {
			logger.debug("Validation failed: Playlist ID not specified.");
			return "The id of the playlist was not specified";
		}

		try {
			Integer.parseInt(playlistString);
			logger.debug("Playlist ID parameter: {}", playlistString);
		} catch (NumberFormatException e) {
			logger.debug("Validation failed: Playlist ID '{}' is not a number.", playlistString);
			return "The playlistId parameter is not a number";
		}

		String[] selectedSongIds = req.getParameterValues("songsSelect");
		if (selectedSongIds == null || selectedSongIds.length == 0) {
			logger.debug("Validation failed: No songs selected.");
			return "You must select at least one song";
		}
		logger.debug("Selected song IDs parameter: {}", Arrays.toString(selectedSongIds));

		List<Integer> songIds = new ArrayList<>();
		try {
			for (String id : selectedSongIds) {
				songIds.add(Integer.parseInt(id));
			}
		} catch (NumberFormatException e) {
			logger.debug("Validation failed: One or more selected song IDs are invalid.");
			return "One or more selected songs have invalid IDs.";
		}
		logger.debug("Parameter validation successful.");
		return null;
	}
}
