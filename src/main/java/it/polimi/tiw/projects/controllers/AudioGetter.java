package it.polimi.tiw.projects.controllers;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.Connection;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.thymeleaf.TemplateEngine;

import it.polimi.tiw.projects.beans.Album;
import it.polimi.tiw.projects.beans.FileData;
import it.polimi.tiw.projects.beans.Song;
import it.polimi.tiw.projects.beans.User;
import it.polimi.tiw.projects.dao.AudioDAO;
import it.polimi.tiw.projects.dao.ImageDAO;
import it.polimi.tiw.projects.dao.SongDAO;
import it.polimi.tiw.projects.exceptions.DAOException;
import it.polimi.tiw.projects.utils.ConnectionHandler;

public class AudioGetter extends HttpServlet {
	private static final Logger logger = LoggerFactory.getLogger(AudioGetter.class);
	static final long serialVersionUID = 1L;
	private static final int DEFAULT_BUFFER_SIZE = 8192;
	private Connection connection = null;

	@Override
	public void init() throws ServletException {
		ServletContext context = getServletContext();
		connection = ConnectionHandler.getConnection(context);
	}
	
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		AudioDAO audioDAO = (AudioDAO) getServletContext().getAttribute("audioDAO");
		SongDAO songDAO = new SongDAO(connection);
		
		// If the user is not logged in (not present in session) redirect to the login
		String loginPath = getServletContext().getContextPath() + "/index.html";
		if (req.getSession().isNew() || req.getSession().getAttribute("user") == null) {
			resp.sendRedirect(loginPath);
			return;
		}
		UUID userId = ((User) req.getSession().getAttribute("user")).getIdUser();
		
		String audioName = req.getParameter("audioName");
		
		//Check Parameter, if the image is not fount nothing happen
		if (audioName == null || audioName.isEmpty()) {
			return;
		}
		
		
		try {
			List<String> userSongs = songDAO.findSongsByUser(userId).stream().map(Song::getAudioFile).toList();
			
			if (!userSongs.stream().anyMatch(img -> img.equals(audioName))) {
				return;
			}	
			
			//TODO if we could add a method in SongDAO "boolean userOwnsAudio(UUID userId, String audoName);" it would be an optimized query
		} catch (DAOException e) {
			logger.error(e.getMessage());
		}
		
		FileData audioFileData = null;
		try {
			audioFileData=audioDAO.getAudio(audioName);
		} catch (IllegalArgumentException e) {
			logger.error("Filename {} is invalid, {}", audioName, e.getMessage());
			return;
		} catch (DAOException e) {
			logger.error("The file is not found, cannot be accessed, or an I/O error occurs");
			return;
		}
		
		if (audioFileData == null || audioFileData.getContent() == null) {
			return;
		}
		
		//Set headers for browsers
		resp.setContentType(audioFileData.getMimeType());
		resp.setContentLengthLong(audioFileData.getSize());
		//inline     -> the user will watch the image immediately
		//attachment -> the user has to do something to watch the image
		//filename   -> used to indicate a fileName if the user wants to save the file
		resp.setHeader("Content-Disposition", "inline; filename=\"" + audioFileData.getFilename() + "\"");

		
        // Disable caching
		resp.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        resp.setDateHeader("Expires", 0);
        
        // We will read the image inputStream in chunks and write it to the response output stream
        // The try-with-resources will automatically flush and close the output stream when done
        try (ServletOutputStream out = resp.getOutputStream()) {
            byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
            int bytesRead;
            
            // Read bytes from the image input stream into the buffer until the end of the stream (-1)
            while ((bytesRead = audioFileData.getContent().read(buffer)) != -1) {
                // Write the buffered bytes to the response output stream, sending them to the client
                out.write(buffer, 0, bytesRead);
            }
        }

	}
		
}
	
	

