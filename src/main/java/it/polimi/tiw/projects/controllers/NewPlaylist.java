package it.polimi.tiw.projects.controllers;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.polimi.tiw.projects.beans.Playlist;
import it.polimi.tiw.projects.beans.User;
import it.polimi.tiw.projects.dao.PlaylistDAO;
import it.polimi.tiw.projects.exceptions.DAOException;
import it.polimi.tiw.projects.listeners.AppContextListener;
import it.polimi.tiw.projects.utils.ConnectionHandler;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.NotNull;

public class NewPlaylist extends HttpServlet {
	private static final Logger logger = LoggerFactory.getLogger(NewPlaylist.class);
	private static final long serialVersionUID = 1L;
	private Connection connection;

	public NewPlaylist() {
		super();
	}

	@Override
	public void init() throws ServletException {
		ServletContext context = getServletContext();
		connection = ConnectionHandler.getConnection(context);
	}

	@Override
	public void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws IOException, ServletException {
		PlaylistDAO playlistDAO = new PlaylistDAO(connection);

		UUID userId = ((User) req.getSession().getAttribute("user")).getIdUser();
		

		// Check Parameters

		String checkResult = areParametersOk(req, getServletContext());

		if (checkResult != null) {
			logger.warn("ParametersNotOk: " + checkResult);
			req.setAttribute("errorNewPlaylistMsg", checkResult);
			req.getRequestDispatcher("/Home").forward(req, resp);
			return;
		}

		// Retrieve Parameters

		String name = req.getParameter("pName").strip();

		List<Integer> songIDs = Arrays.stream(req.getParameterValues("songsSelect"))
				.map(Integer::parseInt).collect(Collectors.toList());

		Playlist playlist;
		try {
			// Search for a playlist with the same name
			List<Integer> listOfPlaylists = playlistDAO.findPlaylistIdsByUser(userId);
			playlist = findPlaylistByName(playlistDAO, listOfPlaylists, name, userId);
		} catch (DAOException e) {
			logger.error("Error in database: {}", e.getMessage(), e);
			resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error in the database");
			return;
		}

		if (playlist == null) {
			// Let's create a new playlist
			try {
				playlistDAO.createPlaylist(name, userId, songIDs);
			} catch (SQLException e) {
				logger.error("Error in database: {}", e.getMessage(), e);
				resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
						"Error in the database");
				return;
			} catch (DAOException e) {
				switch (e.getErrorType()) {
					case NOT_FOUND: {
						req.setAttribute("errorNewPlaylistMsg",
								"One of the song you selected was not found");
						req.getRequestDispatcher("/Home").forward(req, resp);
						return;
					}
					case DUPLICATE_ENTRY: {
						req.setAttribute("errorNewPlaylistMsg",
								"You selected two times the same song");
						req.getRequestDispatcher("/Home").forward(req, resp);
						return;
					}
					default: {
						logger.error("Error in database: {}", e.getMessage(), e);
						resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
								"Error in the database");
						return;
					}
				}
			}
		} else {
			// A playlist with that name already exist
			req.setAttribute("errorNewPlaylistMsg",
					"A playlist named \"" + name + "\" already exists");
			req.getRequestDispatcher("/Home").forward(req, resp);
			return;
		}

		String path = getServletContext().getContextPath() + "/Home";
		resp.sendRedirect(path);

	}

	@Override
	public void destroy() {
		try {
			ConnectionHandler.closeConnection(connection);
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	private Playlist findPlaylistByName(PlaylistDAO dao, List<Integer> list, String name,
			UUID userId) throws DAOException {
		for (int id : list) {
			Playlist playlist = dao.findPlaylistById(id, userId);
			if (playlist.getName().equalsIgnoreCase(name)) {
				return playlist;
			}
		}
		return null;
	}

	private static String areParametersOk(HttpServletRequest req, ServletContext servletContext) {

		Pattern titlePattern = (Pattern) servletContext.getAttribute(AppContextListener.TITLE_REGEX_PATTERN);
		
		String title = req.getParameter("pName");
		if (title == null || (title = title.strip()).isEmpty()) {
			return "You have to choose a name for the playlist";
		}
		if(!isValid(title, titlePattern)) {
			return "Invalid title format. Use letters, numbers, spaces, hyphens, or apostrophes (1-100 characters).";
		}

		String[] selectedSongIds = req.getParameterValues("songsSelect");
		if (selectedSongIds == null || selectedSongIds.length == 0) {
			// No songs selected
			return "You must select at least one song";
		}

		List<Integer> songIds = new ArrayList<>();
		try {
			for (String id : selectedSongIds) {
				songIds.add(Integer.parseInt(id));
			}
		} catch (NumberFormatException e) {
			return "One or more selected songs have invalid IDs.";
		}

		return null;
	}
	
	private static boolean isValid ( @NotNull String parameter, @NotNull Pattern pattern) {
		if (!pattern.matcher(parameter).matches()) {
			return false;
		}
		return true;
	}

}
