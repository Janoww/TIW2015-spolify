package it.polimi.tiw.projects.controllers;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

public class GetPlaylistDetails extends HttpServlet {
	private static final Logger logger = LoggerFactory.getLogger(GetPlaylistDetails.class);
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
	public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
		PlaylistDAO playlistDAO = new PlaylistDAO(connection);
		SongDAO songDAO = new SongDAO(connection);
		AlbumDAO albumDAO = new AlbumDAO(connection);

		UUID userId = ((User) req.getSession().getAttribute("user")).getIdUser();

		// Get and check params
		Integer playlistId = null;
		try {
			playlistId = Integer.parseInt(req.getParameter("playlistId"));
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
			switch (e.getErrorType()) {
			case NOT_FOUND: {
				req.setAttribute("errorOpeningPlaylist", "The playlist you selected was not found");
				req.getRequestDispatcher("/Home").forward(req, resp);
				return;
			}
			case ACCESS_DENIED: {
				req.setAttribute("errorOpeningPlaylist", "The playlist you selected was not found");
				req.getRequestDispatcher("/Home").forward(req, resp);
				return;
			}
			default: {
				e.printStackTrace();
				resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "An internal error occurred");
				return;
			}
			}
		}

		// We need the list of songs in the playlist
		List<SongWithAlbum> songWithAlbumOrdered;
		try {
			songWithAlbumOrdered = orderAllSongs(myPlaylist, songDAO, albumDAO, userId);
		} catch (DAOException e) {
			e.printStackTrace();
			resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "An internal error occurred");
			return;
		}

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

		int totPages = (songWithAlbumOrdered.size() + 4) / 5;

		if (page > totPages - 1) {
			page = totPages - 1;
		}

		List<SongWithAlbum> songWithAlbumDisplayed = songWithAlbumOrdered.stream().skip(page * 5).limit(5).toList();

		// We need the list of not added songs for the form

		List<Song> unusedSongs;
		try {
			unusedSongs = getUnusedSongs(myPlaylist, songDAO, userId);
		} catch (DAOException e) {
			e.printStackTrace();
			resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "An internal error occurred");
			return;
		}

		WebContext ctx = TemplateHandler.getWebContext(req, resp, getServletContext());

		ctx.setVariable("playlist", myPlaylist);
		ctx.setVariable("songWithAlbum", songWithAlbumDisplayed);
		ctx.setVariable("page", page);
		ctx.setVariable("totPages", totPages);
		ctx.setVariable("songs", unusedSongs);
		ctx.setVariable("errorAddSongMsg", req.getParameter("errorAddSongMsg"));

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

	private static List<SongWithAlbum> orderAllSongs(Playlist playlist, SongDAO songDao, AlbumDAO albumDao, UUID userId)
			throws DAOException {

		List<Integer> songsIDs = playlist.getSongs();
		List<Song> songs = songDao.findSongsByIdsAndUser(songsIDs, userId);

		// Pre-load albums
		Map<Integer, Album> albumMap = new HashMap<>();
		for (Song song : songs) {
			int albumId = song.getIdAlbum();
			if (!albumMap.containsKey(albumId)) {
				albumMap.put(albumId, albumDao.findAlbumById(albumId));
			}
		}

		List<SongWithAlbum> result = new ArrayList<>();
		for (Song s : songs) {
			result.add(new SongWithAlbum(s, albumMap.get(s.getIdAlbum())));
		}

		// need to order result

		result.sort(Comparator.comparing((SongWithAlbum swa) -> swa.getAlbum().getArtist().toLowerCase())
				.thenComparing((SongWithAlbum swa) -> swa.getAlbum().getYear()));

		return result;

	}

	private static List<Song> getUnusedSongs(Playlist playlist, SongDAO songDao, UUID userId) throws DAOException {

		List<Song> result = songDao.findSongsByUser(userId);

		List<Integer> alreadyPresentSongsIDs = playlist.getSongs();

		List<Song> toRemove = new ArrayList<>();
		for (Song song : result) {
			if (alreadyPresentSongsIDs.contains(song.getIdSong())) {
				toRemove.add(song);
			}
		}
		result.removeAll(toRemove);

		return result;

	}

}
