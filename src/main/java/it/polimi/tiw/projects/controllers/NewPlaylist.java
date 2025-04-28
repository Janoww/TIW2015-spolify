package it.polimi.tiw.projects.controllers;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import it.polimi.tiw.projects.beans.Playlist;
import it.polimi.tiw.projects.beans.User;
import it.polimi.tiw.projects.dao.PlaylistDAO;
import it.polimi.tiw.projects.dao.SongDAO;
import it.polimi.tiw.projects.exceptions.DAOException;
import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class NewPlaylist extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private Connection connection;

	public void init() {
		try {
			ServletContext context = getServletContext();
			String driver = context.getInitParameter("dbDriver");
			String url = context.getInitParameter("dbUrl");
			String user = context.getInitParameter("dbUser");
			String password = context.getInitParameter("dbPassword");

			Class.forName(driver);

			connection = DriverManager.getConnection(url, user, password);
		} catch (ClassNotFoundException | SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public void doPost(HttpServletRequest req, HttpServletResponse resp) {

		PlaylistDAO playlistDAO = null;
		SongDAO songDAO = null;

		String name = null;
		List<Integer> songIDs = null;
		User user = null;
		Playlist playlist = null;

		try {

			playlistDAO = new PlaylistDAO(connection);
			songDAO = new SongDAO(connection);
			name = req.getParameter("pName");
			
			songIDs = Arrays.asList(req.getParameterValues("songsSelect")).stream().map(Integer::parseInt).toList();

			user = (User) req.getSession().getAttribute("user");

			List<Integer> list = playlistDAO.findPlaylistIdsByUser(user.getIdUser());
			playlist = findPlaylistByName(playlistDAO, list, name, user.getIdUser());

			if (playlist == null) {
			
				System.out.println(songIDs);
				
				
				playlistDAO.createPlaylist(name, null, user.getIdUser(), songIDs);
			} else {
				// TODO
			}

		} catch (Exception e) {
			// TODO
			e.printStackTrace();
		}

		String path = getServletContext().getContextPath() + "/Home";
		try {
			resp.sendRedirect(path);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	private Playlist findPlaylistByName(PlaylistDAO dao, List<Integer> list, String name, UUID userId) {
		return list.stream().map(i -> {
			try {
				return dao.findPlaylistById(i, userId);
			} catch (DAOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return null;
		}).filter(pl -> pl.getName().equalsIgnoreCase(name)).findFirst().orElse(null);
	}

	private List<Integer> findSongIDs(UUID userID, List<String> songs, SongDAO songDAO) {
		try {
			return songDAO.findSongsByUser(userID).stream().filter(i -> songs.contains(i.getTitle()))
					.map(i -> i.getIdSong()).toList();
		} catch (DAOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
}
