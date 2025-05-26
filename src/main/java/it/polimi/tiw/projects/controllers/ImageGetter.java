package it.polimi.tiw.projects.controllers;

import it.polimi.tiw.projects.beans.Album;
import it.polimi.tiw.projects.beans.FileData;
import it.polimi.tiw.projects.beans.User;
import it.polimi.tiw.projects.dao.AlbumDAO;
import it.polimi.tiw.projects.dao.ImageDAO;
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
import java.util.List;
import java.util.UUID;

public class ImageGetter extends HttpServlet {
    static final long serialVersionUID = 1L;
    private static final Logger logger = LoggerFactory.getLogger(ImageGetter.class);
    private static final int DEFAULT_BUFFER_SIZE = 8192;
    private Connection connection = null;

    @Override
    public void init() throws ServletException {
        ServletContext context = getServletContext();
        connection = ConnectionHandler.getConnection(context);
    }

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        ImageDAO imageDAO = (ImageDAO) getServletContext().getAttribute("imageDAO");
        AlbumDAO albumDAO = new AlbumDAO(connection);

        UUID userId = ((User) req.getSession().getAttribute("user")).getIdUser();
        String imageName = req.getParameter("imageName");

        // Check Parameter, if the image is not fount nothing happen
        if (imageName == null || imageName.isEmpty()) {
            return;
        }

        try {
            List<String> userAlbums = albumDAO.findAlbumsByUser(userId).stream().map(Album::getImage).toList();

            if (userAlbums.stream().noneMatch(img -> img.equals(imageName))) {
                return;
            }

            // TODO if we could add a method in AlbumDAO "boolean userOwnsImage(UUID userId,
            // String imageName);" it would be an optimized query
        } catch (DAOException e) {
            logger.error(e.getMessage());
        }

        FileData imageFileData = null;
        try {
            imageFileData = imageDAO.getImage(imageName);
        } catch (IllegalArgumentException e) {
            logger.error("Filename {} is invalid, {}", imageName, e.getMessage());
            return;
        } catch (DAOException e) {
            logger.error("The file is not found, cannot be accessed, or an I/O error occurs");
            return;
        }

        if (imageFileData == null || imageFileData.content() == null) {
            return;
        }

        // Set headers for browsers
        resp.setContentType(imageFileData.mimeType());
        resp.setContentLengthLong(imageFileData.size());
        // inline -> the user will watch the image immediately
        // attachment -> the user has to do something to watch the image
        // filename -> used to indicate a fileName if the user wants to save the file
        resp.setHeader("Content-Disposition", "inline; filename=\"" + imageFileData.filename() + "\"");

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
            while ((bytesRead = imageFileData.content().read(buffer)) != -1) {
                // Write the buffered bytes to the response output stream, sending them to the
                // client
                out.write(buffer, 0, bytesRead);
            }
        }

    }

}
