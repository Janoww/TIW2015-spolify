package it.polimi.tiw.projects.controllers;

import it.polimi.tiw.projects.beans.Album;
import it.polimi.tiw.projects.beans.Song;
import it.polimi.tiw.projects.beans.SongWithAlbum;
import it.polimi.tiw.projects.beans.User;
import it.polimi.tiw.projects.dao.AlbumDAO;
import it.polimi.tiw.projects.dao.SongDAO;
import it.polimi.tiw.projects.exceptions.DAOException;
import it.polimi.tiw.projects.utils.ConnectionHandler;
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
import java.util.List;
import java.util.UUID;

public class OpenSong extends HttpServlet {
    private static final Logger logger = LoggerFactory.getLogger(OpenSong.class);
    private static final long serialVersionUID = 1L;
    private Connection connection;
    private TemplateEngine templateEngine;

    @Override
    public void init() throws ServletException {
        ServletContext context = getServletContext();
        connection = ConnectionHandler.getConnection(context);
        templateEngine = TemplateHandler.initializeEngine(context);
    }

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
        SongDAO songDAO = new SongDAO(connection);
        AlbumDAO albumDAO = new AlbumDAO(connection);

        // If the user is not logged in (not present in session) redirect to the login
        String loginPath = getServletContext().getContextPath() + "/index.html";
        if (req.getSession().isNew() || req.getSession().getAttribute("user") == null) {
            resp.sendRedirect(loginPath);
            return;
        }
        UUID userId = ((User) req.getSession().getAttribute("user")).getIdUser();

        // Get and check params
        Integer playlistId = null;
        Integer songId = null;
        try {
            playlistId = Integer.parseInt(req.getParameter("playlistId"));
        } catch (NumberFormatException | NullPointerException e) {
            logger.error("Error while parsing parameter playlistId: {}", e.getMessage());
            req.setAttribute("errorOpeningPlaylist", "The server doesn't recognise your playlist, retry!");
            req.getRequestDispatcher("/Home").forward(req, resp);
            return;
        }

        try {
            songId = Integer.parseInt(req.getParameter("songId"));
        } catch (NumberFormatException | NullPointerException e) {
            logger.error("Error while parsing parameter songId: {}", e.getMessage());
            req.setAttribute("errorOpeningSong", "An error occurred while trying opening the song, retry!");
            req.getRequestDispatcher("/GetPlaylistDetails").forward(req, resp);
            return;
        }

        // Find song and album
        Song song;
        Album album;
        try {
            song = songDAO.findSongsByIdsAndUser(List.of(songId), userId).getFirst();
            album = albumDAO.findAlbumById(song.getIdAlbum());
        } catch (DAOException e) {
            logger.error("DAO exception: {}", e.getMessage());
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "An internal error occurred");
            return;
        }

        SongWithAlbum swa = new SongWithAlbum(song, album);

        WebContext ctx = TemplateHandler.getWebContext(req, resp, getServletContext());

        ctx.setVariable("playlistId", playlistId);
        ctx.setVariable("swa", swa);

        String path = "/WEB-INF/SongInspector.html";
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
}
