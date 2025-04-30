package it.polimi.tiw.projects.controllers;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.WebContext;

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
import it.polimi.tiw.projects.utils.TemplateHandler;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class GetPlaylistDetails extends HttpServlet{
	private static final long serialVersionUID = 1L;
	private Connection connection;
	private TemplateEngine templateEngine;
	
	public GetPlaylistDetails() {
		super();
	}

	@Override
	public void init() throws ServletException {
		ServletContext context = getServletContext();
		connection = ConnectionHandler.getConnection(context);
		templateEngine = TemplateHandler.initializeEngine(context);

	}
	
	@Override
	public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		PlaylistDAO playlistDAO = new PlaylistDAO(connection);
		SongDAO songDAO = new SongDAO(connection);
		AlbumDAO albumDAO = new AlbumDAO(connection);
		UUID userId = ((User) req.getSession().getAttribute("user")).getIdUser();
		
		// If the user is not logged in (not present in session) redirect to the login
		String loginpath = getServletContext().getContextPath() + "/index.html";
		if (req.getSession().isNew() || req.getSession().getAttribute("user") == null) {
			resp.sendRedirect(loginpath);
			return;
		}
		
		// Get and check params
		Integer playlistId = null;
		try {
			playlistId = Integer.parseInt(req.getParameter("missionid"));
		} catch (NumberFormatException | NullPointerException e) {
			e.printStackTrace();
			resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Incorrect param values");
			return;
		}
		
		// If a playlist with that id exists for that user, find it
		Playlist myPlaylist;
		try {
			myPlaylist = playlistDAO.findPlaylistById(playlistId, userId);
		} catch (DAOException e) {
			switch (e.getErrorType()){
				case NOT_FOUND:{
					req.setAttribute("errorOpeningPlaylist", "The playlist you selected was not found");
					req.getRequestDispatcher("/Home");
					return;
				}
				case ACCESS_DENIED:{
					req.setAttribute("errorOpeningPlaylist", "The playlist you selected was not found");
					req.getRequestDispatcher("/Home");
					return;
				}
				default:{
					e.printStackTrace();
					resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "An internal error occurred");
					return;
				}
			}
		}
		
		
		// We need the list of songs
		List<Song> allSongsOrdered = orderAllSongs(myPlaylist, songDAO, albumDAO);
		
		// We divide the playlist in pages of 5 songs
		// We need to understand which page we need to render:
		
		Integer page = 0;
	    try {
	    	page = Integer.parseInt(req.getParameter("page"));
	        if (page < 0) {
	        	page = 0;
	        }
	    } catch (NumberFormatException e) {
	    	page = 0;
	    }
		
	    int totalSongs = allSongsOrdered.size();
	    
	    List<Song> songsPage = allSongsOrdered.stream()
	            .skip(page*5)
	            .limit(5)
	            .toList();
	    
	    List<SongWithAlbum> songWithAlbum = null;
	    
	    songWithAlbum = songsPage.stream().map(s -> {
	    	try {
				return new SongWithAlbum(s, albumDAO.findAlbumById(s.getIdAlbum()));
			} catch (DAOException e) {
				e.printStackTrace();
			}
			return null;
	    }).toList();
	    

		WebContext ctx = TemplateHandler.getWebContext(req, resp, getServletContext());

		ctx.setVariable("playlist", myPlaylist);
		ctx.setVariable("songWithAlbum", songWithAlbum);
		ctx.setVariable("page", page);
		ctx.setVariable("totalSongs", totalSongs);
		
		String path = "/WEB-INF/Playlist.html";
		templateEngine.process(path, ctx, resp.getWriter());
		

	}
	
	@Override
	public void destroy() {
		try {
			ConnectionHandler.closeConnection(connection);
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	private static List<Song> orderAllSongs(Playlist playlist, SongDAO songDao, AlbumDAO albumDao){
		
		List<Song> result = null;
		
		List<Integer> songsIDs = playlist.getSongs();
		//TODO
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		return result;
	}
	

}

