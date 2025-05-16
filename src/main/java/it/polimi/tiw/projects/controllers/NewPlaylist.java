package it.polimi.tiw.projects.controllers;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.polimi.tiw.projects.beans.Playlist;
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

@WebServlet("/NewPlaylist")
public class NewPlaylist extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private static final Logger logger = LoggerFactory.getLogger(NewPlaylist.class);
	private Connection connection;

	public NewPlaylist() {
		super();
	}

	@Override
	public void init() throws ServletException {
		ServletContext context = getServletContext();
		connection = ConnectionHandler.getConnection(context);
		logger.info("NewPlaylist servlet initialized.");
	}

	@Override
	public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		logger.debug("Received POST request to create a new playlist.");
		PlaylistDAO playlistDAO = new PlaylistDAO(connection);
		User user = (User) req.getSession().getAttribute("user");

		if (user == null) {
			logger.warn("User not authenticated. Denying access to create playlist.");
			resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
			resp.setContentType("application/json");
			resp.setCharacterEncoding("UTF-8");
			ObjectMapper mapper = new ObjectMapper();
			Map<String, String> errorResponse = new HashMap<>();
			errorResponse.put("error", "User not authenticated");
			resp.getWriter().write(mapper.writeValueAsString(errorResponse));
			return;
		}
		logger.debug("User {} (ID: {}) authenticated.", user.getUsername(), user.getIdUser());

		String checkResult = areParametersOk(req);
		if (checkResult != null) {
			logger.warn("Parameter validation failed for new playlist creation: {}", checkResult);
			resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			resp.setContentType("application/json");
			resp.setCharacterEncoding("UTF-8");
			ObjectMapper mapper = new ObjectMapper();
			Map<String, String> errorResponse = new HashMap<>();
			errorResponse.put("error", checkResult);
			resp.getWriter().write(mapper.writeValueAsString(errorResponse));
			return;
		}
		logger.debug("Parameters validated successfully for new playlist.");

		String name = req.getParameter("pName").strip();
		List<Integer> songIDs = Arrays.stream(req.getParameterValues("songsSelect"))
				.map(Integer::parseInt).collect(Collectors.toList());
		logger.debug("Attempting to create playlist with name: '{}', song IDs: {} for user ID: {}",
				name, songIDs, user.getIdUser());

		Playlist existingPlaylistByName;
		try {
			List<Integer> listOfPlaylists = playlistDAO.findPlaylistIdsByUser(user.getIdUser());
			logger.debug("User {} has {} existing playlists. Checking for name conflict with '{}'.",
					user.getUsername(), listOfPlaylists.size(), name);
			existingPlaylistByName =
					findPlaylistByName(playlistDAO, listOfPlaylists, name, user.getIdUser());
		} catch (DAOException e) {
			logger.error(
					"DAOException while checking for existing playlist name '{}' for user ID: {}. ErrorType: {}",
					name, user.getIdUser(), e.getErrorType(), e);
			resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			resp.setContentType("application/json");
			resp.setCharacterEncoding("UTF-8");
			ObjectMapper mapper = new ObjectMapper();
			Map<String, String> errorResponse = new HashMap<>();
			errorResponse.put("error", "Error checking for existing playlist");
			resp.getWriter().write(mapper.writeValueAsString(errorResponse));
			return;
		}

		if (existingPlaylistByName != null) {
			logger.warn(
					"Playlist name conflict: A playlist named '{}' already exists for user ID: {}",
					name, user.getIdUser());
			resp.setStatus(HttpServletResponse.SC_CONFLICT);
			resp.setContentType("application/json");
			resp.setCharacterEncoding("UTF-8");
			ObjectMapper mapper = new ObjectMapper();
			Map<String, String> errorResponse = new HashMap<>();
			errorResponse.put("error", "A playlist named '" + name + "' already exists");
			resp.getWriter().write(mapper.writeValueAsString(errorResponse));
			return;
		}
		logger.debug("No playlist name conflict found for '{}' for user ID: {}", name,
				user.getIdUser());

		Playlist createdPlaylist;
		try {
			createdPlaylist = playlistDAO.createPlaylist(name, user.getIdUser(), songIDs);
			if (createdPlaylist == null) {
				logger.error(
						"playlistDAO.createPlaylist returned null for name '{}', user ID: {}. This should not happen if DAO throws exceptions.",
						name, user.getIdUser());
				throw new DAOException(
						"Failed to create playlist or retrieve its details post-creation.",
						DAOException.DAOErrorType.GENERIC_ERROR);
			}
			logger.info("Successfully created playlist ID: {} with name '{}' for user ID: {}",
					createdPlaylist.getIdPlaylist(), name, user.getIdUser());
		} catch (SQLException e) {
			logger.error("SQLException during playlist creation for name '{}', user ID: {}.", name,
					user.getIdUser(), e);
			resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			resp.setContentType("application/json");
			resp.setCharacterEncoding("UTF-8");
			ObjectMapper mapper = new ObjectMapper();
			Map<String, String> errorResponse = new HashMap<>();
			errorResponse.put("error", "Database error during playlist creation");
			resp.getWriter().write(mapper.writeValueAsString(errorResponse));
			return;
		} catch (DAOException e) {
			String errorMessage = "Error creating playlist";
			int statusCode = HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
			switch (e.getErrorType()) {
				case NOT_FOUND:
					errorMessage = "One of the selected songs was not found";
					statusCode = HttpServletResponse.SC_BAD_REQUEST;
					logger.warn(
							"Song not found during playlist creation for name '{}', user ID: {}. Details: {}",
							name, user.getIdUser(), e.getMessage());
					break;
				case DUPLICATE_ENTRY:
					errorMessage = "Duplicate song entry for the new playlist";
					statusCode = HttpServletResponse.SC_BAD_REQUEST;
					logger.warn(
							"Duplicate song entry during playlist creation for name '{}', user ID: {}. Details: {}",
							name, user.getIdUser(), e.getMessage());
					break;
				case NAME_ALREADY_EXISTS:
					errorMessage = "A playlist named '" + name + "' already exists";
					statusCode = HttpServletResponse.SC_CONFLICT;
					logger.warn(
							"Playlist name already exists (caught during createPlaylist) for name '{}', user ID: {}. Details: {}",
							name, user.getIdUser(), e.getMessage());
					break;
				default:
					logger.error(
							"DAOException during playlist creation for name '{}', user ID: {}. ErrorType: {}. Details: {}",
							name, user.getIdUser(), e.getErrorType(), e.getMessage(), e);
					// errorMessage and statusCode remain as defaults (Internal Server Error)
					if (e.getErrorType() == DAOException.DAOErrorType.GENERIC_ERROR
							&& e.getMessage() != null && !e.getMessage().equals(errorMessage)) {
						errorMessage = e.getMessage(); // Use more specific message if available for
														// GENERIC_ERROR
					}
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

		resp.setStatus(HttpServletResponse.SC_CREATED);
		resp.setContentType("application/json");
		resp.setCharacterEncoding("UTF-8");
		ObjectMapper mapper = new ObjectMapper();
		resp.getWriter().write(mapper.writeValueAsString(createdPlaylist));
		logger.debug("Successfully sent CREATED response for new playlist ID: {}",
				createdPlaylist.getIdPlaylist());
	}

	@Override
	public void destroy() {
		try {
			ConnectionHandler.closeConnection(connection);
			logger.info("NewPlaylist servlet destroyed. Connection closed.");
		} catch (SQLException e) {
			logger.error("Failed to close database connection on servlet destroy.", e);
		}
	}

	private Playlist findPlaylistByName(PlaylistDAO dao, List<Integer> list, String name,
			UUID userId) throws DAOException {
		logger.debug("Searching for playlist by name '{}' among {} playlists for user ID: {}", name,
				list.size(), userId);
		for (int id : list) {
			Playlist playlist = dao.findPlaylistById(id, userId);
			if (playlist.getName().equalsIgnoreCase(name)) {
				logger.debug("Found existing playlist with name '{}' and ID: {} for user ID: {}",
						name, id, userId);
				return playlist;
			}
		}
		logger.debug("No playlist found with name '{}' for user ID: {}", name, userId);
		return null;
	}

	private static String areParametersOk(HttpServletRequest req) {
		logger.debug("Validating parameters for new playlist creation.");
		String title = req.getParameter("pName");
		if (title == null || (title = title.strip()).isEmpty()) {
			logger.debug("Validation failed: Playlist name not specified.");
			return "You have to choose a name for the playlist";
		}
		logger.debug("Playlist name parameter: {}", title);

		String[] selectedSongIds = req.getParameterValues("songsSelect");
		if (selectedSongIds == null || selectedSongIds.length == 0) {
			logger.debug("Validation failed: No songs selected for the new playlist.");
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
		logger.debug("Parameter validation successful for new playlist.");
		return null;
	}

}
