package it.polimi.tiw.projects.controllers;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.WebContext;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;

public class GoToHome extends HttpServlet {
    static final long serialVersionUID = 1L;
    private static final Logger logger = LoggerFactory.getLogger(GoToHome.class);
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

        UUID userId = ((User) req.getSession().getAttribute("user")).getIdUser();

        List<Integer> playlistIDs = null;
        List<Song> songList = null;
        List<Genre> genresList = Arrays.asList(Genre.values());

        List<Playlist> playlists = null;
        try {
            playlists = playlistDAO.findPlaylistsByUser(userId);
            songList = songDAO.findSongsByUser(userId);
            logger.debug("Searched for songs and playlists");
        } catch (DAOException e) {
            logger.error(e.getMessage(), e);
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error in the database");
            return;
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
