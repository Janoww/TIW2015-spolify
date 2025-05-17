package it.polimi.tiw.projects.controllers;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.polimi.tiw.projects.beans.Album;
import it.polimi.tiw.projects.beans.Song;
import it.polimi.tiw.projects.beans.User;
import it.polimi.tiw.projects.dao.AlbumDAO;
import it.polimi.tiw.projects.dao.SongDAO;
import it.polimi.tiw.projects.exceptions.DAOException;
import it.polimi.tiw.projects.utils.ConnectionHandler;
import it.polimi.tiw.projects.utils.Genre;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;

@WebServlet("/NewSong")
@MultipartConfig
public class NewSong extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private static final Logger logger = LoggerFactory.getLogger(NewSong.class);
	private Connection connection = null;

	public NewSong() {
		super();
	}

	@Override
	public void init() throws ServletException {
		ServletContext context = getServletContext();
		connection = ConnectionHandler.getConnection(context);
		logger.info("NewSong servlet initialized.");
	}

	@Override
	public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
		logger.debug("Received POST request to create a new song.");
		SongDAO songDAO = new SongDAO(connection);
		AlbumDAO albumDAO = new AlbumDAO(connection);
		User user = (User) req.getSession().getAttribute("user");

		if (user == null) {
			logger.warn("User not authenticated. Denying access to create song.");
			resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
			resp.setContentType("application/json");
			resp.setCharacterEncoding("UTF-8");
			ObjectMapper mapper = new ObjectMapper();
			Map<String, String> errorResponse = new HashMap<>();
			errorResponse.put("error", "User not authenticated");
			resp.getWriter().write(mapper.writeValueAsString(errorResponse));
			return;
		}
		logger.debug("User {} (ID: {}) authenticated.", user.getUsername(), user.getIdUser());

		String checkResult = areParametersOk(req);
		if (checkResult != null) {
			logger.warn("Parameter validation failed for new song creation: {}", checkResult);
			resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			resp.setContentType("application/json");
			resp.setCharacterEncoding("UTF-8");
			ObjectMapper mapper = new ObjectMapper();
			Map<String, String> errorResponse = new HashMap<>();
			errorResponse.put("error", checkResult);
			resp.getWriter().write(mapper.writeValueAsString(errorResponse));
			return;
		}
		logger.debug("Parameters validated successfully for new song.");

		String title = req.getParameter("sTitle").strip();
		String albumName = req.getParameter("sAlbum").strip();
		Integer year = Integer.valueOf(req.getParameter("sYear").strip());
		String artist = req.getParameter("sArtist").strip();
		Genre genre;
		try {
			genre = Genre.valueOf(req.getParameter("sGenre").strip());
			logger.debug("Parsed genre: {}", genre);
		} catch (IllegalArgumentException | NullPointerException e) {
			logger.warn("Invalid genre value received: '{}' for user ID: {}. Details: {}", req.getParameter("sGenre"),
					user.getIdUser(), e.getMessage());
			resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			resp.setContentType("application/json");
			resp.setCharacterEncoding("UTF-8");
			ObjectMapper mapper = new ObjectMapper();
			Map<String, String> errorResponse = new HashMap<>();
			errorResponse.put("error", "Invalid genre value");
			resp.getWriter().write(mapper.writeValueAsString(errorResponse));
			return;
		}
		logger.debug("New song details: Title='{}', Album='{}', Year={}, Artist='{}', Genre='{}', UserID={}", title,
				albumName, year, artist, genre, user.getIdUser());

		List<Album> albums;
		try {
			albums = albumDAO.findAlbumsByUser(user.getIdUser());
			logger.debug("User {} has {} existing albums. Checking for album '{}'.", user.getUsername(), albums.size(),
					albumName);
		} catch (DAOException e) {
			logger.error("DAOException while fetching albums for user ID: {}. ErrorType: {}", user.getIdUser(),
					e.getErrorType(), e);
			resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			resp.setContentType("application/json");
			resp.setCharacterEncoding("UTF-8");
			ObjectMapper mapper = new ObjectMapper();
			Map<String, String> errorResponse = new HashMap<>();
			errorResponse.put("error", "Error fetching album data");
			resp.getWriter().write(mapper.writeValueAsString(errorResponse));
			return;
		}

		Album existingAlbum = findAlbum(albums, albumName);

		if (existingAlbum != null
				&& (!(existingAlbum.getYear() == year) || !existingAlbum.getArtist().equalsIgnoreCase(artist))) {
			logger.warn(
					"Album conflict: Album '{}' (ID: {}) exists with different year/artist. Submitted: Year={}, Artist='{}'. Existing: Year={}, Artist='{}'. User ID: {}",
					albumName, existingAlbum.getIdAlbum(), year, artist, existingAlbum.getYear(),
					existingAlbum.getArtist(), user.getIdUser());
			String conflictMsg = "An album named " + escapeJson(existingAlbum.getName())
					+ " already exists with different year/artist.";
			resp.setStatus(HttpServletResponse.SC_CONFLICT);
			resp.setContentType("application/json");
			resp.setCharacterEncoding("UTF-8");
			ObjectMapper mapper = new ObjectMapper();
			Map<String, String> errorResponse = new HashMap<>();
			errorResponse.put("error", conflictMsg);
			resp.getWriter().write(mapper.writeValueAsString(errorResponse));
			return;
		}
		if (existingAlbum != null)
			logger.debug("Found existing album: ID={}, Name='{}'", existingAlbum.getIdAlbum(), existingAlbum.getName());
		else
			logger.debug("No existing album found with name '{}'. A new one will be created.", albumName);

		try {
			if (existingAlbum != null) {
				List<Song> songsInAlbum = songDAO.findSongsByUser(user.getIdUser()).stream()
						.filter(s -> s.getIdAlbum() == existingAlbum.getIdAlbum()).collect(Collectors.toList());
				logger.debug("Album ID {} (Name: '{}') has {} songs by user {}.", existingAlbum.getIdAlbum(),
						existingAlbum.getName(), songsInAlbum.size(), user.getUsername());

				if (songsInAlbum.stream().anyMatch(s -> s.getTitle().equalsIgnoreCase(title))) {
					logger.warn("Song conflict: Song titled '{}' already exists in album '{}' (ID: {}) for user ID: {}",
							title, existingAlbum.getName(), existingAlbum.getIdAlbum(), user.getIdUser());
					String conflictMsg = "The song titled \"" + escapeJson(title) + "\" in album \""
							+ escapeJson(existingAlbum.getName()) + "\" already exists.";
					resp.setStatus(HttpServletResponse.SC_CONFLICT);
					resp.setContentType("application/json");
					resp.setCharacterEncoding("UTF-8");
					ObjectMapper mapper = new ObjectMapper();
					Map<String, String> errorResponse = new HashMap<>();
					errorResponse.put("error", conflictMsg);
					resp.getWriter().write(mapper.writeValueAsString(errorResponse));
					return;
				}
				logger.debug("No song title conflict for '{}' in album '{}' (ID: {})", title, existingAlbum.getName(),
						existingAlbum.getIdAlbum());
			}
		} catch (DAOException e) {
			logger.error("DAOException while checking for existing songs in album '{}' for user ID: {}. ErrorType: {}",
					albumName, user.getIdUser(), e.getErrorType(), e);
			resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			resp.setContentType("application/json");
			resp.setCharacterEncoding("UTF-8");
			ObjectMapper mapper = new ObjectMapper();
			Map<String, String> errorResponse = new HashMap<>();
			errorResponse.put("error", "Error checking for existing songs");
			resp.getWriter().write(mapper.writeValueAsString(errorResponse));
			return;
		}

		// TODO: Implement actual file saving and get real URLs
		// For now, using placeholders. In a real app, AudioDAO/ImageDAO would be used
		// here.
		String imageUrl = "placeholder_image_url.jpg";
		String audioUrl = "placeholder_audio_url.mp3";
		logger.debug("Using placeholder URLs: Image='{}', Audio='{}'", imageUrl, audioUrl);

		Album albumToUse = existingAlbum;
		if (albumToUse == null) {
			logger.debug("Creating new album: Name='{}', Year={}, Artist='{}', Cover='{}', UserID={}", albumName, year,
					artist, imageUrl, user.getIdUser());
			try {
				albumToUse = albumDAO.createAlbum(albumName, year, artist, imageUrl, user.getIdUser());
				if (albumToUse == null) { // Should not happen if DAO throws exceptions
					logger.error("albumDAO.createAlbum returned null for '{}'. This indicates an issue in DAO.",
							albumName);
					throw new DAOException("Failed to create or retrieve album.",
							DAOException.DAOErrorType.GENERIC_ERROR);
				}
				logger.info("Successfully created new album ID: {} with name '{}'", albumToUse.getIdAlbum(),
						albumToUse.getName());
			} catch (DAOException e) {
				logger.error("DAOException while creating new album '{}' for user ID: {}. ErrorType: {}", albumName,
						user.getIdUser(), e.getErrorType(), e);
				resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
				resp.setContentType("application/json");
				resp.setCharacterEncoding("UTF-8");
				ObjectMapper mapper = new ObjectMapper();
				Map<String, String> errorResponse = new HashMap<>();
				errorResponse.put("error", "Error creating new album");
				resp.getWriter().write(mapper.writeValueAsString(errorResponse));
				return;
			}
		}

		it.polimi.tiw.projects.beans.Song createdSong;
		try {
			logger.debug("Creating new song: Title='{}', AlbumID={}, Year={}, Genre={}, Audio='{}', UserID={}", title,
					albumToUse.getIdAlbum(), year, genre, audioUrl, user.getIdUser());
			createdSong = songDAO.createSong(title, albumToUse.getIdAlbum(), year, genre, audioUrl, user.getIdUser());
			if (createdSong == null) { // Should not happen if DAO throws exceptions
				logger.error("songDAO.createSong returned null for title '{}'. This indicates an issue in DAO.", title);
				throw new DAOException("Failed to create or retrieve song.", DAOException.DAOErrorType.GENERIC_ERROR);
			}
			logger.info("Successfully created new song ID: {} with title '{}'", createdSong.getIdSong(),
					createdSong.getTitle());
		} catch (DAOException e) {
			logger.error("DAOException while creating new song '{}' for user ID: {}. ErrorType: {}", title,
					user.getIdUser(), e.getErrorType(), e);
			resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			resp.setContentType("application/json");
			resp.setCharacterEncoding("UTF-8");
			ObjectMapper mapper = new ObjectMapper();
			Map<String, String> errorResponse = new HashMap<>();
			errorResponse.put("error", "Error creating new song: " + e.getMessage());
			resp.getWriter().write(mapper.writeValueAsString(errorResponse));
			return;
		}

		resp.setStatus(HttpServletResponse.SC_CREATED);
		resp.setContentType("application/json");
		resp.setCharacterEncoding("UTF-8");

		ObjectMapper mapper = new ObjectMapper();
		Map<String, Object> responseData = new HashMap<>();

		Map<String, Object> songMap = new HashMap<>();
		songMap.put("idSong", createdSong.getIdSong());
		songMap.put("title", createdSong.getTitle());
		songMap.put("genre", createdSong.getGenre().name());
		songMap.put("year", createdSong.getYear());
		songMap.put("audioUrl", audioUrl);

		Map<String, Object> albumMap = new HashMap<>();
		albumMap.put("idAlbum", albumToUse.getIdAlbum());
		albumMap.put("name", albumToUse.getName());
		albumMap.put("artist", albumToUse.getArtist());
		albumMap.put("year", albumToUse.getYear());
		albumMap.put("coverUrl", imageUrl);

		responseData.put("song", songMap);
		responseData.put("album", albumMap);

		resp.getWriter().write(mapper.writeValueAsString(responseData));
		logger.debug("Successfully sent CREATED response with new song (ID: {}) and album (ID: {}) details.",
				createdSong.getIdSong(), albumToUse.getIdAlbum());
	}

	private String escapeJson(String str) {
		if (str == null)
			return "";
		return str.replace("\\", "\\\\").replace("\"", "\\\"").replace("\b", "\\b").replace("\f", "\\f")
				.replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
	}

	@Override
	public void destroy() {
		try {
			ConnectionHandler.closeConnection(connection);
			logger.info("NewSong servlet destroyed. Connection closed.");
		} catch (SQLException e) {
			logger.error("Failed to close database connection on servlet destroy.", e);
		}
	}

	private static Album findAlbum(List<Album> list, String albumName) {
		logger.trace("Searching for album by name '{}' in a list of {} albums.", albumName, list.size());
		Album found = list.stream().filter(a -> a.getName().equalsIgnoreCase(albumName)).findFirst().orElse(null);
		if (found != null) {
			logger.trace("Album '{}' found with ID: {}", albumName, found.getIdAlbum());
		} else {
			logger.trace("Album '{}' not found in the list.", albumName);
		}
		return found;
	}

	private static String areParametersOk(HttpServletRequest req) {
		logger.debug("Validating parameters for new song creation.");
		try {
			String title = req.getParameter("sTitle");
			if (title == null || (title = title.strip()).isEmpty()) {
				logger.debug("Validation failed: Song title not specified.");
				return "You have to choose a title";
			}
			logger.trace("Song title parameter: {}", title);

			String albumName = req.getParameter("sAlbum");
			if (albumName == null || (albumName = albumName.strip()).isEmpty()) {
				logger.debug("Validation failed: Album name not specified.");
				return "You have to specify the album name";
			}
			logger.trace("Album name parameter: {}", albumName);

			String yearString = req.getParameter("sYear");
			if (yearString == null || (yearString = yearString.strip()).isEmpty()) {
				logger.debug("Validation failed: Album year not specified.");
				return "You have to specify the album's year of release";
			}
			try {
				Integer.parseInt(yearString);
				logger.trace("Year parameter: {}", yearString);
			} catch (NumberFormatException e) {
				logger.debug("Validation failed: Year '{}' is not a valid number.", yearString);
				return "The year must be a valid number";
			}

			String artist = req.getParameter("sArtist");
			if (artist == null || (artist = artist.strip()).isEmpty()) {
				logger.debug("Validation failed: Artist name not specified.");
				return "You have to specify the name of the artist";
			}
			logger.trace("Artist parameter: {}", artist);

			String genreName = req.getParameter("sGenre");
			if (genreName == null || (genreName = genreName.strip()).isEmpty()) {
				logger.debug("Validation failed: Genre not specified.");
				return "You must choose a genre from the predefined ones";
			}
			try {
				Genre.valueOf(genreName);
				logger.trace("Genre parameter: {}", genreName);
			} catch (IllegalArgumentException e) {
				logger.debug("Validation failed: Genre '{}' is not a predefined value.", genreName);
				return "You must choose a genre from the predefined ones";
			}

			Part imagePart = req.getPart("sIcon");
			if (imagePart == null || imagePart.getSize() == 0) {
				logger.debug("Validation failed: Image file not uploaded.");
				return "You must upload an image";
			}
			String imageType = imagePart.getContentType();
			if (imageType == null || !imageType.startsWith("image/")) {
				logger.debug("Validation failed: Uploaded image file type '{}' is not valid.", imageType);
				return "The uploaded file must be a valid image";
			}
			logger.trace("Image part content type: {}", imageType);

			Part audioPart = req.getPart("sFile");
			if (audioPart == null || audioPart.getSize() == 0) {
				logger.debug("Validation failed: Audio file not uploaded.");
				return "You must upload an audio file";
			}
			String audioType = audioPart.getContentType();
			if (audioType == null || !audioType.startsWith("audio/")) {
				logger.debug("Validation failed: Uploaded audio file type '{}' is not valid.", audioType);
				return "The uploaded file must be a valid audio file";
			}
			logger.trace("Audio part content type: {}", audioType);

		} catch (IOException | ServletException e) {
			logger.error("Error while processing form data during parameter validation.", e);
			return "Error while processing form data";
		}
		logger.debug("Parameter validation successful for new song.");
		return null;
	}
}
