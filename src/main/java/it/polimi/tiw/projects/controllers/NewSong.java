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
import it.polimi.tiw.projects.exceptions.DAOException;
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
	public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		SongDAO songDAO = new SongDAO(connection);
		AlbumDAO albumDAO = new AlbumDAO(connection);

		String title = req.getParameter("sTitle").strip();
		String albumName = req.getParameter("sAlbum").strip();
		Integer year = Integer.valueOf(req.getParameter("sYear").strip());
		String artist = req.getParameter("sArtist").strip();
		String genre = req.getParameter("sGenre").strip();
		String image = req.getParameter("sIcon"); // FIXME
		String audioFile = req.getParameter("sFile"); // FIXME
		
		User user = (User) req.getSession().getAttribute("user");

		if(user != null) {
			//Trovo la lista degli album
			List<Album> albums;
			try {
				albums = albumDAO.findAlbumsByUser(user.getIdUser());
			} catch (DAOException e) {
				e.printStackTrace();
				resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error in the database");
				return;
			}
			//Cerco l'album di interesse
			Album album = findAlbum(albums, albumName);
			
			if (album != null && (!(album.getYear() == year) || !album.getArtist().equalsIgnoreCase(artist))) {
				//Se l'album esiste ma le informazioni fornite non coincidono:
				
				//TODO
			}
			
			if (album == null) {
				try {
					album = albumDAO.createAlbum(albumName, year, artist, user.getIdUser());
				} catch (DAOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			int idAlbum = album.getIdAlbum();

			try {
				songDAO.createSong(title, idAlbum, year, genre, audioFile, user.getIdUser());
			} catch (DAOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			
		} else {
			resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Session lost");
			return;
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
