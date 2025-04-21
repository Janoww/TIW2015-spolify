package it.polimi.tiw.projects.controllers;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.List;

import it.polimi.tiw.projects.beans.Album;
import it.polimi.tiw.projects.beans.User;
import it.polimi.tiw.projects.dao.AlbumDAO;
import it.polimi.tiw.projects.dao.SongDAO;
import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class NewSong extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private Connection connection = null;

	@Override
	public void init() {

		try {
			ServletContext context = getServletContext();
			String driver = context.getInitParameter("dbDriver");
			String url = context.getInitParameter("dbUrl");
			String user = context.getInitParameter("dbUser");
			String password = context.getInitParameter("dbPassword");

			Class.forName(driver);

			connection = DriverManager.getConnection(url, user, password);
		} catch (Exception e) {
			// TODO
		}

	}

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

	private static Album findAlbum(List<Album> list, String albumName) {
		return list.stream().filter(a -> a.getName().equalsIgnoreCase(albumName)).findFirst().orElse(null);
	}

}
