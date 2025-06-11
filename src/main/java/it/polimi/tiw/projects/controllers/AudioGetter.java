package it.polimi.tiw.projects.controllers;

import it.polimi.tiw.projects.beans.FileData;
import it.polimi.tiw.projects.beans.Song;
import it.polimi.tiw.projects.beans.User;
import it.polimi.tiw.projects.dao.AudioDAO;
import it.polimi.tiw.projects.dao.SongDAO;
import it.polimi.tiw.projects.exceptions.DAOException;
import it.polimi.tiw.projects.utils.ConnectionHandler;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

public class AudioGetter extends HttpServlet {
    static final long serialVersionUID = 1L;
    private static final Logger logger = LoggerFactory.getLogger(AudioGetter.class);
    private static final int DEFAULT_BUFFER_SIZE = 8192;
    private Connection connection = null;

    @Override
    public void init() throws ServletException {
        ServletContext context = getServletContext();
        connection = ConnectionHandler.getConnection(context);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        AudioDAO audioDAO = (AudioDAO) getServletContext().getAttribute("audioDAO");
        SongDAO songDAO = new SongDAO(connection);

        UUID userId = ((User) req.getSession().getAttribute("user")).getIdUser();

        String audioName = req.getParameter("audioName");

        // Check Parameter, if the image is not fount nothing happen
        if (audioName == null || audioName.isEmpty()) {
            logger.warn("audioName is Empty or null");
            resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        try {
            List<String> userSongs = songDAO.findSongsByUser(userId).stream().map(Song::getAudioFile).toList();

            if (userSongs.stream().noneMatch(aud -> aud.equals(audioName))) {
                resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                return;
            }

        } catch (DAOException e) {
            logger.error("Database error {}", e.getMessage(), e);
        }

        FileData audioFileData = null;
        try {
            audioFileData = audioDAO.getAudio(audioName);
        } catch (IllegalArgumentException e) {
            logger.error("Filename {} is invalid, {}", audioName, e.getMessage());
            resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        } catch (DAOException e) {
            logger.error("The file is not found, cannot be accessed, or an I/O error occurs");
            resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        if (audioFileData == null || audioFileData.content() == null) {
            logger.warn("The audio file is null or empty");
            resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        logger.info("Retrieved the audioFileData for audio {}: ", audioFileData.filename());

        // Set headers for browsers
        resp.setContentType(audioFileData.mimeType());
        resp.setContentLengthLong(audioFileData.size());
        // inline -> the user will watch the image immediately
        // attachment -> the user has to do something to watch the image
        // filename -> used to indicate a fileName if the user wants to save the file
        resp.setHeader("Content-Disposition", "inline; filename=\"" + audioFileData.filename() + "\"");

        // Disable caching
        resp.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        resp.setDateHeader("Expires", 0);

        // We will read the image inputStream in chunks and write it to the response
        // output stream
        // The try-with-resources will automatically flush and close the output stream
        // when done
        try (ServletOutputStream out = resp.getOutputStream()) {
            byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
            int bytesRead;

            // Read bytes from the image input stream into the buffer until the end of the
            // stream (-1)
            while ((bytesRead = audioFileData.content().read(buffer)) != -1) {
                // Write the buffered bytes to the response output stream, sending them to the
                // client
                out.write(buffer, 0, bytesRead);
            }
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
