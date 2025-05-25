package it.polimi.tiw.projects.controllers;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.polimi.tiw.projects.beans.User;
import it.polimi.tiw.projects.dao.PlaylistDAO;
import it.polimi.tiw.projects.exceptions.DAOException;
import it.polimi.tiw.projects.utils.ConnectionHandler;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.NotNull;

public class AddSongToPL extends HttpServlet {
	private static final Logger logger = LoggerFactory.getLogger(AddSongToPL.class);
	private static final long serialVersionUID = 1L;
	private Connection connection;

	public AddSongToPL() {
		super();
	}

	@Override
	public void init() throws ServletException {
		ServletContext context = getServletContext();
		connection = ConnectionHandler.getConnection(context);
	}

	@Override
	public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
		PlaylistDAO playlistDAO = new PlaylistDAO(connection);
		UUID userId = ((User) req.getSession().getAttribute("user")).getIdUser();

		// Check parameter
		String checkResult = areParametersOk(req);

		if (checkResult != null) {
			logger.info("Parameters are not ok: {}", checkResult);
			req.setAttribute("errorAddSongMsg", checkResult);
			req.getRequestDispatcher("/GetPlaylistDetails").forward(req, resp);
			return;
		}

		// Retrieve Parameters

		List<Integer> songIDs = Arrays.stream(req.getParameterValues("songsSelect")).map(Integer::parseInt)
				.collect(Collectors.toList());
		Integer playlistId = Integer.parseInt(req.getParameter("playlistId"));

		try {
			for (Integer id : songIDs) {
				playlistDAO.addSongToPlaylist(playlistId, userId, id);
			}
		} catch (DAOException e) {
			switch (e.getErrorType()) {
			case NOT_FOUND: {
				req.setAttribute("errorAddSongMsg", e.getMessage());
				req.getRequestDispatcher("/GetPlaylistDetails").forward(req, resp);
				return;
			}
			case ACCESS_DENIED: {
				req.setAttribute("errorAddSongMsg", "The playlist was not fount");
				req.getRequestDispatcher("/GetPlaylistDetails").forward(req, resp);
				return;
			}
			case DUPLICATE_ENTRY: {
				req.setAttribute("errorAddSongMsg", e.getMessage());
				req.getRequestDispatcher("/GetPlaylistDetails").forward(req, resp);
				return;
			}
			default: {
				logger.error("Unknown error", e);
				resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error in the database");
				return;
			}
			}
		}

		String path = getServletContext().getContextPath() + "/GetPlaylistDetails?playlistId=" + playlistId;
		resp.sendRedirect(path);
	}

	@Override
	public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
		this.doPost(req, resp);
	}

	@Override
	public void destroy() {
		try {
			ConnectionHandler.closeConnection(connection);
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	private String areParametersOk(HttpServletRequest req) {

		String playlistString = req.getParameter("playlistId");
		if (playlistString == null || (playlistString = playlistString.strip()).isEmpty()) {
			return "The id of the playlist was not specified";
		}

		try {
			Integer.parseInt(playlistString);
		} catch (NumberFormatException e) {
			return "The playlistId parameter is not a number";
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

		logger.info("All the parameters are valid");
		return null;
	}
}
