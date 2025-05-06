package it.polimi.tiw.projects.controllers;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

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
	private Connection connection;

	public GoToHome() {
		super();
	}

	@Override
	public void init() throws ServletException {
		ServletContext context = getServletContext();
		connection = ConnectionHandler.getConnection(context);
	}

	@Override
	public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		PlaylistDAO playlistDAO = new PlaylistDAO(connection);
		SongDAO songDAO = new SongDAO(connection);
		UUID userId = ((User) req.getSession().getAttribute("user")).getIdUser();

		List<Integer> playlistIDs = null;
		List<Song> songList = null;
		List<Genre> genresList = Arrays.asList(Genre.values());

		try {
			playlistIDs = playlistDAO.findPlaylistIdsByUser(userId);
			songList = songDAO.findSongsByUser(userId);
		} catch (DAOException e) {
			e.printStackTrace();
			resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error in the database");
			return;
		}

		// Get the list of all playlists
		List<Playlist> playlists = playlistIDs.stream().map(id -> {
			try {
				return playlistDAO.findPlaylistById(id, userId);
			} catch (DAOException e) {
				e.printStackTrace();
				return null;
			}
		}).toList();

		// TODO: Send JSON response with playlists, songs, genres
		resp.setContentType("application/json");
		resp.setCharacterEncoding("UTF-8");
		// Example: Serialize playlists, songList, genresList into a JSON object
		// ObjectMapper mapper = new ObjectMapper();
		// Map<String, Object> data = new HashMap<>();
		// data.put("playlists", playlists);
		// data.put("songs", songList);
		// data.put("genres", genresList);
		// String jsonResponse = mapper.writeValueAsString(data);
		// resp.getWriter().write(jsonResponse);
		resp.getWriter().write("{\"message\": \"Home data will be here\"}"); // Placeholder
	}

	@Override
	public void destroy() {
		try {
			ConnectionHandler.closeConnection(connection);
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
}
