package it.polimi.tiw.projects.controllers;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.WebContext;
import it.polimi.tiw.projects.beans.Playlist;
import it.polimi.tiw.projects.beans.Song;
import it.polimi.tiw.projects.beans.User;
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

public class GoToHome extends HttpServlet {
	static final long serialVersionUID = 1L;
	private Connection connection;
	private TemplateEngine templateEngine;
	
	public GoToHome() {
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

		UUID userId = ((User) req.getSession().getAttribute("user")).getIdUser();

		try {
			List<Integer> playlistIDs = playlistDAO.findPlaylistIdsByUser(userId);
			
			List<Playlist> playslists = playlistIDs.stream().map(id -> {
				try {
					return playlistDAO.findPlaylistById(id, userId);
				} catch (DAOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					return null;
				}
			}).toList();
			
			List<Song> songList = songDAO.findSongsByUser(userId);
			
			WebContext ctx = TemplateHandler.getWebContext(req, resp, getServletContext());
			
			ctx.setVariable("playlists", playslists);
			ctx.setVariable("songs", songList);
			
			String path = "/WEB-INF/Home.html";
			templateEngine.process(path, ctx, resp.getWriter());

			
		} catch (DAOException e) {
			e.printStackTrace();
			resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error in the database");
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
}
