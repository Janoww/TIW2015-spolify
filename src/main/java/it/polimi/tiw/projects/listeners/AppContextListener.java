package it.polimi.tiw.projects.listeners;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;

import it.polimi.tiw.projects.dao.AudioDAO;
import it.polimi.tiw.projects.dao.ImageDAO;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Enumeration;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import com.mysql.cj.jdbc.AbandonedConnectionCleanupThread;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

@WebListener
public class AppContextListener implements ServletContextListener {

    private static final Logger logger = LoggerFactory.getLogger(AppContextListener.class);
    private HikariDataSource dataSource;

    // Keys for ServletContext attributes for validation patterns
    public static final String USERNAME_REGEX_PATTERN = "USERNAME_REGEX_PATTERN";
    public static final String NAME_REGEX_PATTERN = "NAME_REGEX_PATTERN";
    public static final String PASSWORD_MIN_LENGTH = "PASSWORD_MIN_LENGTH";
    public static final String PASSWORD_MAX_LENGTH = "PASSWORD_MAX_LENGTH";

    // Consolidated text validation parameters
    public static final String STANDARD_TEXT_REGEX_PATTERN = "STANDARD_TEXT_REGEX_PATTERN";
    public static final String STANDARD_TEXT_MIN_LENGTH = "STANDARD_TEXT_MIN_LENGTH";
    public static final String STANDARD_TEXT_MAX_LENGTH = "STANDARD_TEXT_MAX_LENGTH";

    // Playlist name has a specific regex
    public static final String PLAYLIST_NAME_REGEX_PATTERN = "PLAYLIST_NAME_REGEX_PATTERN";

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        ServletContext context = sce.getServletContext();

        // Configure HikariCP
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(context.getInitParameter("dbUrl"));
        config.setUsername(context.getInitParameter("dbUser"));
        config.setPassword(context.getInitParameter("dbPassword"));
        config.setDriverClassName(context.getInitParameter("dbDriver"));
        // Configuration recommended by HikariCP for mysql
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        config.addDataSourceProperty("useServerPrepStmts", "true");
        config.addDataSourceProperty("useLocalSessionState", "true");
        config.addDataSourceProperty("rewriteBatchedStatements", "true");
        config.addDataSourceProperty("cacheResultSetMetadata", "true");
        config.addDataSourceProperty("cacheServerConfiguration", "true");
        config.addDataSourceProperty("elideSetAutoCommits", "true");
        config.addDataSourceProperty("maintainTimeStats", "false");

        // Create the DataSource
        try {
            // Create the DataSource
            dataSource = new HikariDataSource(config);

            // Store the DataSource in the ServletContext
            context.setAttribute("dataSource", dataSource);

            logger.info("HikariCP DataSource initialized and added to ServletContext.");
        } catch (Exception e) {
            logger.error("!!! FAILED TO INITIALIZE HIKARI DATASOURCE !!!", e);
            throw new RuntimeException("Failed to initialize database connection pool", e);
        }

        // Create singleton ImageDAO and AudioDAO
        String homeDirectory = System.getProperty("user.home"); // Get user's home directory
        if (homeDirectory == null) {
            logger.error("!!! FAILED TO GET USER HOME DIRECTORY (user.home property) !!!");
            throw new RuntimeException("Could not determine user home directory for storage setup.");
        }
        Path storageBasePath = Paths.get(homeDirectory, "Spolify");
        logger.info("Base storage path for DAOs set to: {}", storageBasePath);

        try {
            ImageDAO imageDAO = new ImageDAO(storageBasePath);
            AudioDAO audioDAO = new AudioDAO(storageBasePath);

            context.setAttribute("imageDAO", imageDAO);
            context.setAttribute("audioDAO", audioDAO);

            logger.info("ImageDAO and AudioDAO singletons created and added to ServletContext.");
        } catch (RuntimeException e) {
            logger.error("!!! FAILED TO INITIALIZE ImageDAO or AudioDAO !!!", e);
            throw new RuntimeException("Failed to initialize file storage DAOs", e);
        }

        // Load and compile validation patterns
        loadAndStoreValidationPatterns(context);
    }

    private void loadAndStoreValidationPatterns(ServletContext context) {
        logger.info("Loading validation patterns from web.xml...");

        // Username Regex
        String usernameRegexStr = context.getInitParameter("regex.username");
        if (usernameRegexStr != null && !usernameRegexStr.isBlank()) {
            try {
                Pattern usernamePattern = Pattern.compile(usernameRegexStr);
                context.setAttribute(USERNAME_REGEX_PATTERN, usernamePattern);
                logger.info("Loaded and compiled username regex pattern: {}", usernameRegexStr);
            } catch (PatternSyntaxException e) {
                logger.error("Invalid regex syntax for username pattern (regex.username): '{}'. Error: {}",
                        usernameRegexStr, e.getMessage());
            }
        } else {
            logger.warn(
                    "Username regex pattern (regex.username) not found or empty in web.xml. Username validation might be affected.");
        }

        // Name Regex (for name and surname)
        String nameRegexStr = context.getInitParameter("regex.name");
        if (nameRegexStr != null && !nameRegexStr.isBlank()) {
            try {
                Pattern namePattern = Pattern.compile(nameRegexStr);
                context.setAttribute(NAME_REGEX_PATTERN, namePattern);
                logger.info("Loaded and compiled name/surname regex pattern: {}", nameRegexStr);
            } catch (PatternSyntaxException e) {
                logger.error("Invalid regex syntax for name pattern (regex.name): '{}'. Error: {}", nameRegexStr,
                        e.getMessage());
            }
        } else {
            logger.warn(
                    "Name regex pattern (regex.name) not found or empty in web.xml. Name/surname validation might be affected.");
        }

        // Password Minimum Length
        String passwordMinLengthStr = context.getInitParameter("validation.password.minLength");
        if (passwordMinLengthStr != null && !passwordMinLengthStr.isBlank()) {
            try {
                int minLength = Integer.parseInt(passwordMinLengthStr);
                if (minLength > 0) {
                    context.setAttribute(PASSWORD_MIN_LENGTH, minLength);
                    logger.info("Loaded password minimum length: {}", minLength);
                } else {
                    logger.warn(
                            "Password minimum length (validation.password.minLength) must be a positive integer, but was: {}. Using default or skipping length check.",
                            passwordMinLengthStr);
                }
            } catch (NumberFormatException e) {
                logger.error(
                        "Invalid number format for password minimum length (validation.password.minLength): '{}'. Error: {}",
                        passwordMinLengthStr, e.getMessage());
            }
        } else {
            logger.warn(
                    "Password minimum length (validation.password.minLength) not found or empty in web.xml. Password length validation might be affected.");
        }

        // Password Maximum Length
        String passwordMaxLengthStr = context.getInitParameter("validation.password.maxLength");
        if (passwordMaxLengthStr != null && !passwordMaxLengthStr.isBlank()) {
            try {
                int maxLength = Integer.parseInt(passwordMaxLengthStr);
                if (maxLength > 0) {
                    context.setAttribute(PASSWORD_MAX_LENGTH, maxLength);
                    logger.info("Loaded password maximum length: {}", maxLength);
                } else {
                    logger.warn(
                            "Password maximum length (validation.password.maxLength) must be a positive integer, but was: {}. Using default or skipping length check.",
                            passwordMaxLengthStr);
                }
            } catch (NumberFormatException e) {
                logger.error(
                        "Invalid number format for password maximum length (validation.password.maxLength): '{}'. Error: {}",
                        passwordMaxLengthStr, e.getMessage());
            }
        } else {
            logger.warn(
                    "Password maximum length (validation.password.maxLength) not found or empty in web.xml. Password length validation might be affected.");
        }

        // Standard Text Regex
        String standardTextRegexStr = context.getInitParameter("regex.standardText");
        if (standardTextRegexStr != null && !standardTextRegexStr.isBlank()) {
            try {
                Pattern standardTextPattern = Pattern.compile(standardTextRegexStr);
                context.setAttribute(STANDARD_TEXT_REGEX_PATTERN, standardTextPattern);
                logger.info("Loaded and compiled standard text regex pattern: {}", standardTextRegexStr);
            } catch (PatternSyntaxException e) {
                logger.error("Invalid regex syntax for standard text pattern (regex.standardText): '{}'. Error: {}",
                        standardTextRegexStr, e.getMessage());
            }
        } else {
            logger.warn(
                    "Standard text regex pattern (regex.standardText) not found or empty in web.xml. Validation for song titles, album titles, etc., might be affected.");
        }

        // Standard Text Minimum Length
        String standardTextMinLengthStr = context.getInitParameter("validation.standardText.minLength");
        if (standardTextMinLengthStr != null && !standardTextMinLengthStr.isBlank()) {
            try {
                int minLength = Integer.parseInt(standardTextMinLengthStr);
                if (minLength >= 0) { // Allow 0 for min length if needed, though 1 is typical
                    context.setAttribute(STANDARD_TEXT_MIN_LENGTH, minLength);
                    logger.info("Loaded standard text minimum length: {}", minLength);
                } else {
                    logger.warn(
                            "Standard text minimum length (validation.standardText.minLength) must be non-negative, but was: {}. Using default or skipping length check.",
                            standardTextMinLengthStr);
                }
            } catch (NumberFormatException e) {
                logger.error(
                        "Invalid number format for standard text minimum length (validation.standardText.minLength): '{}'. Error: {}",
                        standardTextMinLengthStr, e.getMessage());
            }
        } else {
            logger.warn(
                    "Standard text minimum length (validation.standardText.minLength) not found or empty in web.xml. Length validation might be affected.");
        }

        // Standard Text Maximum Length
        String standardTextMaxLengthStr = context.getInitParameter("validation.standardText.maxLength");
        if (standardTextMaxLengthStr != null && !standardTextMaxLengthStr.isBlank()) {
            try {
                int maxLength = Integer.parseInt(standardTextMaxLengthStr);
                if (maxLength > 0) {
                    context.setAttribute(STANDARD_TEXT_MAX_LENGTH, maxLength);
                    logger.info("Loaded standard text maximum length: {}", maxLength);
                } else {
                    logger.warn(
                            "Standard text maximum length (validation.standardText.maxLength) must be a positive integer, but was: {}. Using default or skipping length check.",
                            standardTextMaxLengthStr);
                }
            } catch (NumberFormatException e) {
                logger.error(
                        "Invalid number format for standard text maximum length (validation.standardText.maxLength): '{}'. Error: {}",
                        standardTextMaxLengthStr, e.getMessage());
            }
        } else {
            logger.warn(
                    "Standard text maximum length (validation.standardText.maxLength) not found or empty in web.xml. Length validation might be affected.");
        }

        // Playlist Name Regex (specific regex, uses standard min/max length)
        String playlistNameRegexStr = context.getInitParameter("regex.playlistName");
        if (playlistNameRegexStr != null && !playlistNameRegexStr.isBlank()) {
            try {
                Pattern playlistNamePattern = Pattern.compile(playlistNameRegexStr);
                context.setAttribute(PLAYLIST_NAME_REGEX_PATTERN, playlistNamePattern);
                logger.info("Loaded and compiled playlist name regex pattern: {}", playlistNameRegexStr);
            } catch (PatternSyntaxException e) {
                logger.error("Invalid regex syntax for playlist name pattern (regex.playlistName): '{}'. Error: {}",
                        playlistNameRegexStr, e.getMessage());
            }
        } else {
            logger.warn("Playlist name regex pattern (regex.playlistName) not found or empty in web.xml.");
        }

        logger.info("Validation patterns loading complete.");
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        // Close the DataSource when the application shuts down
        if (dataSource != null) {
            dataSource.close();
            logger.info("HikariCP DataSource closed.");
        }

        // Tomcat gives a warning for memory leak for a thread created by jdbc to
        // Resolve: Deregister JDBC drivers loaded by this webapp's classloader
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        Enumeration<Driver> drivers = DriverManager.getDrivers();
        while (drivers.hasMoreElements()) {
            Driver driver = drivers.nextElement();
            if (driver.getClass().getClassLoader() == cl) {
                try {
                    logger.info("Unregistering JDBC driver: {}", driver);
                    DriverManager.deregisterDriver(driver);
                } catch (SQLException ex) {
                    logger.error("Error unregistering JDBC driver: {}", driver, ex);
                }
            } else {
                logger.debug("Skipping deregistration for driver not loaded by webapp: {}", driver);
            }
        }
        logger.info("JDBC Driver deregistration process finished.");

        // Explicitly shut down the MySQL abandoned connection cleanup thread
        try {
            logger.info("Attempting to shut down MySQL AbandonedConnectionCleanupThread...");
            AbandonedConnectionCleanupThread.checkedShutdown();
            logger.info("MySQL AbandonedConnectionCleanupThread shutdown successful.");
        } catch (Exception e) {
            logger.error("Error shutting down MySQL AbandonedConnectionCleanupThread:", e);
        }
    }
}
