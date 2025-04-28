package it.polimi.tiw.projects.controllers;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;

import it.polimi.tiw.projects.beans.Album;
import it.polimi.tiw.projects.beans.User;
import it.polimi.tiw.projects.dao.AlbumDAO;
import it.polimi.tiw.projects.dao.SongDAO;
import it.polimi.tiw.projects.utils.ConnectionHandler;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class NewSong extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private Connection connection = null;
	
	public NewSong() {
		super();
	}

	@Override
	public void init() throws ServletException{
			ServletContext context = getServletContext();
			connection = ConnectionHandler.getConnection(context);
	}

	@Override
	public void doPost(HttpServletRequest req, HttpServletResponse resp) {
		SongDAO songDAO = null;
		AlbumDAO albumDAO = null;

		String title = null;
		String albumName = null;
		Integer year = null;
		String artist = null;
		String genre = null;
		String image = null;
		String audioFile = null;

		try {
			title = req.getParameter("sTitle");
			albumName = req.getParameter("sAlbum");
			year = Integer.valueOf(req.getParameter("sYear"));
			artist = req.getParameter("sArtist");
			genre = req.getParameter("sGenre");
			image = req.getParameter("sIcon"); // FIXME
			audioFile = req.getParameter("sFile"); // FIXME
		} catch (Exception e) {
			// TODO
		}

		try {
			songDAO = new SongDAO(connection);
			albumDAO = new AlbumDAO(connection);

			User user = (User) req.getSession().getAttribute("user");
			if (user != null) {
				List<Album> albums = albumDAO.findAlbumsByUser(user.getIdUser());
				Album album = findAlbum(albums, albumName);

				if (album != null && (!(album.getYear() == year) || !album.getArtist().equalsIgnoreCase(artist))) {
					// TODO
				}

				if (album == null) {
					album = albumDAO.createAlbum(albumName, year, artist, user.getIdUser());
				}
				int idAlbum = album.getIdAlbum();

				songDAO.createSong(title, idAlbum, year, genre, audioFile, user.getIdUser());

			} else {

				// TODO
			}
		} catch (Exception e) {
			// TODO
		}

		String path = getServletContext().getContextPath() + "/Home";
		try {
			resp.sendRedirect(path);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
	
	@Override
	public void destroy() {
		try {
			ConnectionHandler.closeConnection(connection);
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	private static Album findAlbum(List<Album> list, String albumName) {
		return list.stream().filter(a -> a.getName().equalsIgnoreCase(albumName)).findFirst().orElse(null);
	}

}
