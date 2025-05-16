package it.polimi.tiw.projects.controllers;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.WebContext;
import it.polimi.tiw.projects.beans.Playlist;
import it.polimi.tiw.projects.beans.Song;
import it.polimi.tiw.projects.beans.User;
import it.polimi.tiw.projects.dao.PlaylistDAO;
import it.polimi.tiw.projects.dao.SongDAO;
import it.polimi.tiw.projects.exceptions.DAOException;
import it.polimi.tiw.projects.utils.ConnectionHandler;
import it.polimi.tiw.projects.utils.Genre;
import it.polimi.tiw.projects.utils.TemplateHandler;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class GoToHome extends HttpServlet {
	private static final Logger logger = LoggerFactory.getLogger(GoToHome.class);
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
		logger.debug("Loading HOME");
		PlaylistDAO playlistDAO = new PlaylistDAO(connection);
		SongDAO songDAO = new SongDAO(connection);
		
		// If the user is not logged in (not present in session) redirect to the login
		String loginPath = getServletContext().getContextPath() + "/index.html";
		if (req.getSession().isNew() || req.getSession().getAttribute("user") == null) {
			resp.sendRedirect(loginPath);
			return;
		}
		UUID userId = ((User) req.getSession().getAttribute("user")).getIdUser();
	

		List<Integer> playlistIDs = null;
		List<Song> songList = null;
		List<Genre> genresList = Arrays.asList(Genre.values());

		try {
			playlistIDs = playlistDAO.findPlaylistIdsByUser(userId);
			songList = songDAO.findSongsByUser(userId);
			logger.debug("Searched for songs and playlists");
		} catch (DAOException e) {
			logger.error(e.getMessage(), e);
			resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error in the database");
			return;
		}

		// Get the list of all playlists
		
		//FIXME playlists have to be ordered "ordinate per data di creazione decrescente"
		List<Playlist> playlists = null;
		if (playlistIDs != null && !playlistIDs.isEmpty()) {
			playlists = playlistIDs.stream().map(id -> {
				try {
					return playlistDAO.findPlaylistById(id, userId);
				} catch (DAOException e) {
					logger.error(e.getMessage(), e);
					return null;
				}
			}).toList();
		}

		WebContext ctx = TemplateHandler.getWebContext(req, resp, getServletContext());

		ctx.setVariable("playlists", playlists);
		ctx.setVariable("songs", songList);
		ctx.setVariable("genres", genresList);

		ctx.setVariable("errorNewPlaylistMsg", req.getAttribute("errorNewPlaylistMsg"));
		ctx.setVariable("errorNewSongMsg", req.getAttribute("errorNewSongMsg"));
		ctx.setVariable("errorOpeningPlaylist", req.getAttribute("errorOpeningPlaylist"));

		String path = "/WEB-INF/Home.html";
		templateEngine.process(path, ctx, resp.getWriter());

	}

	@Override
	public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		User user = (User) req.getSession().getAttribute("user");
		logger.debug("UUID user: " + user.getIdUser());
		this.doGet(req, resp);
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
