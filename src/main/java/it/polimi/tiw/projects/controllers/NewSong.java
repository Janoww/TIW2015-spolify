package it.polimi.tiw.projects.controllers;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.polimi.tiw.projects.beans.Album;
import it.polimi.tiw.projects.beans.User;
import it.polimi.tiw.projects.dao.AlbumDAO;
import it.polimi.tiw.projects.dao.AudioDAO;
import it.polimi.tiw.projects.dao.ImageDAO;
import it.polimi.tiw.projects.dao.PlaylistDAO;
import it.polimi.tiw.projects.dao.SongDAO;
import it.polimi.tiw.projects.exceptions.DAOException;
import it.polimi.tiw.projects.listeners.AppContextListener;
import it.polimi.tiw.projects.utils.ConnectionHandler;
import it.polimi.tiw.projects.utils.Genre;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;
import jakarta.validation.constraints.NotNull;


@MultipartConfig
public class NewSong extends HttpServlet {
	private static final Logger logger = LoggerFactory.getLogger(NewSong.class);
	private static final long serialVersionUID = 1L;
	private Connection connection = null;

	public NewSong() {
		super();
	}

	@Override
	public void init() throws ServletException {
		ServletContext context = getServletContext();
		connection = ConnectionHandler.getConnection(context);
	}

	@Override
	public void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws IOException, ServletException {
		logger.debug("processing the Post request");
		SongDAO songDAO = new SongDAO(connection);
		AlbumDAO albumDAO = new AlbumDAO(connection);

		// Check Parameters
		String checkResult = areParametersOk(req, getServletContext());

		if (checkResult != null) {
			req.setAttribute("errorNewSongMsg", checkResult);
			logger.warn("ParametersNotOk: " + checkResult);
			req.getRequestDispatcher("/Home").forward(req, resp);
			return;
		}

		logger.info("Parameters are ok");

		// Retrieve parameters
		String title = req.getParameter("sTitle").strip();
		String albumName = req.getParameter("sAlbum").strip();
		Integer year = Integer.valueOf(req.getParameter("sYear").strip());
		String artist = req.getParameter("sArtist").strip();
		Genre genre;
		try {
			genre = Genre.valueOf((req.getParameter("sGenre").strip()));
		} catch (IllegalArgumentException | NullPointerException e) {
			req.setAttribute("errorNewSongMsg", "You must choose a genre from the predefined ones");
			req.getRequestDispatcher("/Home").forward(req, resp);
			return;
		}
		Part imagePart = req.getPart("sIcon");
		Part audioPart = req.getPart("sFile");


		logger.debug("Retrieved Parameters");

		User user = (User) req.getSession().getAttribute("user");

		if (user != null) {
			// Find the list of all the albums
			List<Album> albums;
			try {
				albums = albumDAO.findAlbumsByUser(user.getIdUser());
			} catch (DAOException e) {
				e.printStackTrace();
				resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
						"Error in the database");
				return;
			}

			// Search if there is already an album with that name
			Album album = findAlbum(albums, albumName);

			if (album != null && (!(album.getYear() == year)
					|| !album.getArtist().equalsIgnoreCase(artist))) {
				// If an album with that name already exists but the information don't match
				req.setAttribute("errorNewSongMsg", "An album named \"" + album.getName()
						+ "\" already exists, it is from the year " + year + " by \"" + album.getArtist() + "\"");

				req.getRequestDispatcher("/Home").forward(req, resp);
				return;
			}

			// An album can't have songs with the same name
			try {
				if (album != null && songDAO.findSongsByUser(user.getIdUser()).stream()
						.anyMatch(s -> s.getTitle().equals(title))) {
					req.setAttribute("errorNewSongMsg",
							"The song titled \"" + title + "\" of the album \"" + album.getName()
									+ "\" have already been uploaded");
					req.getRequestDispatcher("/Home").forward(req, resp);
					return;
				}
			} catch (DAOException e) {
				e.printStackTrace();
				resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
						"Error in the database");
				return;
			}

			String imageFileRename = null;

			if (album == null) {
				// If an album already exists the image will not be updated

				String imageFileName =
						Paths.get(imagePart.getSubmittedFileName()).getFileName().toString();

				ImageDAO imageDAO = (ImageDAO) getServletContext().getAttribute("imageDAO");

				try {
					try (InputStream imageStream = imagePart.getInputStream()){
						imageFileRename = imageDAO.saveImage(imageStream, imageFileName);
					}
				} catch (IllegalArgumentException e) {
					resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
							"The image file you provvided may be corrupted");
					logger.error("Problem with the imageFIle: {}", e.getMessage(), e);
					return;
				} catch (DAOException e) {
					resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
							"Failed to save image due to I/O error");
					logger.error("I/O error: {}", e.getMessage(), e);
					return;
				}

			}

			Boolean isAlbumNew = false;
			// Create a new album if it doesn't exist
			if (album == null) {
				try {
					album = albumDAO.createAlbum(albumName, year, artist, imageFileRename,
							user.getIdUser());
					isAlbumNew = true;
				} catch (DAOException e) {
					resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
							"Error in the database");
					logger.error("Error creating album: {}", e.getMessage(), e);
					return;
				}
			}
			int idAlbum = album.getIdAlbum();

			// Saving the audio file


			AudioDAO audioDAO = (AudioDAO) getServletContext().getAttribute("audioDAO");

			String audioFileName =
					Paths.get(audioPart.getSubmittedFileName()).getFileName().toString();

			String audioFileRename;
			try {
				try(InputStream audioStream = audioPart.getInputStream()){
					audioFileRename = audioDAO.saveAudio(audioStream, audioFileName);
				}
			} catch (IllegalArgumentException e) {
				resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
						"The audio file you provided may be corrupted");
				logger.error("Audio file broken: {}", e.getMessage(), e);
				return;
			} catch (DAOException e) {
				resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
						"Failed to save image due to I/O error");
				logger.error("Error saving the audio: {}", e.getMessage(), e);
				return;
			}

			try {
				songDAO.createSong(title, idAlbum, year, genre, audioFileRename, user.getIdUser());
			} catch (DAOException e) {
				resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
						"Error in the database");
				logger.error("An error occurred while creating the song: {}", e.getMessage(), e);
				//Deleting the album
				if (isAlbumNew) {
					try {
						albumDAO.deleteAlbum(idAlbum, user.getIdUser());
					} catch (DAOException e1) {
						logger.error("While trying to delate an album because of the failed creation of the song an error occurred: {}", e1.getMessage());
					}
				}
				return;
			}

		} else {
			// The user was not retrieved
			resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Session lost");
			return;
		}

		String path = getServletContext().getContextPath() + "/Home";
		resp.sendRedirect(path);

	}

	@Override
	public void destroy() {
		try {
			ConnectionHandler.closeConnection(connection);
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	private static Album findAlbum(List<Album> list, String albumName) {
		return list.stream().filter(a -> a.getName().equalsIgnoreCase(albumName)).findFirst()
				.orElse(null);
	}

	private static String areParametersOk(HttpServletRequest req, ServletContext servletContext) {

		Pattern titlePattern = (Pattern) servletContext.getAttribute(AppContextListener.TITLE_REGEX_PATTERN);
		
		try {
			String title = req.getParameter("sTitle");
			if (title == null || (title = title.strip()).isEmpty()) {
				return "You have to choose a title";
			}
			if(!isValid(title, titlePattern)) {
				return "Invalid title format. Use letters, numbers, spaces, hyphens, or apostrophes (1-100 characters).";
			}

			String albumName = req.getParameter("sAlbum");
			if (albumName == null || (albumName = albumName.strip()).isEmpty()) {
				return "You have to specify the album name";
			}
			if(!isValid(title, titlePattern)) {
				return "Invalid album name format. Use letters, numbers, spaces, hyphens, or apostrophes (1-100 characters).";
			}

			String yearString = req.getParameter("sYear");
			if (yearString == null || (yearString = yearString.strip()).isEmpty()) {
				return "You have to specify the album's year of release";
			}
			try {
				Integer.parseInt(yearString);
			} catch (NumberFormatException e) {
				return "The year must be a valid number";
			}

			String artist = req.getParameter("sArtist");
			if (artist == null || (artist = artist.strip()).isEmpty()) {
				return "You have to specify the name of the artist";
			}
			if(!isValid(title, titlePattern)) {
				return "Invalid artis name format. Use letters, numbers, spaces, hyphens, or apostrophes (1-100 characters).";
			}

			String genreName = req.getParameter("sGenre");
			if (genreName == null || (genreName = genreName.strip()).isEmpty()) {
				return "You must choose a genre from the predefined ones";
			}
			try {
				Genre.valueOf(genreName);
			} catch (IllegalArgumentException e) {
				return "You must choose a genre from the predefined ones";
			}

			Part imagePart = req.getPart("sIcon");
			if (imagePart == null || imagePart.getSize() == 0) {
				return "You must upload an image";
			}
			String imageType = imagePart.getContentType();
			if (imageType == null || !imageType.startsWith("image/")) {
				return "The uploaded file must be a valid image";
			}

			Part audioPart = req.getPart("sFile");
			if (audioPart == null || audioPart.getSize() == 0) {
				return "You must upload an audio file";
			}
			String audioType = audioPart.getContentType();
			if (audioType == null || !audioType.startsWith("audio/")) {
				return "The uploaded file must be a valid audio file";
			}

		} catch (IOException | ServletException e) {
			return "Error while processing form data";
		}

		return null; // all good
	}
	
	private static boolean isValid ( @NotNull String parameter, @NotNull Pattern pattern) {
		if (!pattern.matcher(parameter).matches()) {
			return false;
		}
		return true;
	}
}
