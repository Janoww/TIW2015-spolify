package it.polimi.tiw.projects.listeners;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;

import it.polimi.tiw.projects.dao.AudioDAO;
import it.polimi.tiw.projects.dao.ImageDAO;
import it.polimi.tiw.projects.dao.PlaylistOrderDAO;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Enumeration;
import java.util.function.IntPredicate;
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
        String homeDirectory = System.getProperty("user.home");
        if (homeDirectory == null) {
            logger.error("!!! FAILED TO GET USER HOME DIRECTORY (user.home property) !!!");
            throw new RuntimeException("Could not determine user home directory for storage setup.");
        }
        Path storageBasePath = Paths.get(homeDirectory, "Spolify");
        logger.info("Base storage path for DAOs set to: {}", storageBasePath);

        try {
            ImageDAO imageDAO = new ImageDAO(storageBasePath);
            AudioDAO audioDAO = new AudioDAO(storageBasePath);
            PlaylistOrderDAO playlistOrderDAO = new PlaylistOrderDAO(storageBasePath);

            context.setAttribute("imageDAO", imageDAO);
            context.setAttribute("audioDAO", audioDAO);
            context.setAttribute("playlistOrderDAO", playlistOrderDAO);

            logger.info("ImageDAO, AudioDAO and PlaylistOrderDAO singletons created and added to ServletContext.");

        } catch (RuntimeException e) {
            logger.error("!!! FAILED TO INITIALIZE ImageDAO, AudioDAO or PlaylistOrderDAO !!!", e);
            throw new RuntimeException("Failed to initialize file storage DAOs", e);
        }

        // Load and compile validation patterns
        loadAndStoreValidationPatterns(context);
    }

    private void loadRegexPattern(ServletContext context, String paramName, String attributeKey,
            String patternDescriptionForLogs) {
        String regexStr = context.getInitParameter(paramName);
        if (regexStr != null && !regexStr.isBlank()) {
            try {
                Pattern pattern = Pattern.compile(regexStr);
                context.setAttribute(attributeKey, pattern);
                logger.info("Loaded and compiled {} regex pattern: {}", patternDescriptionForLogs, regexStr);
            } catch (PatternSyntaxException e) {
                logger.error("Invalid regex syntax for {} pattern ({}): '{}'. Error: {}", patternDescriptionForLogs,
                        paramName, regexStr, e.getMessage());
            }
        } else {
            logger.warn("{} regex pattern ({}) not found or empty in web.xml.", patternDescriptionForLogs, paramName);
        }
    }

    private void loadIntegerValidation(ServletContext context, String paramName, String attributeKey,
            String valueDescriptionForLogs, IntPredicate validator, String validationFailureMessage) {
        String valueStr = context.getInitParameter(paramName);
        if (valueStr != null && !valueStr.isBlank()) {
            try {
                int value = Integer.parseInt(valueStr);
                if (validator.test(value)) {
                    context.setAttribute(attributeKey, value);
                    logger.info("Loaded {}: {}", valueDescriptionForLogs, value);
                } else {
                    logger.warn("{} ({}) {}. Value was: {}. Using default or skipping check.", valueDescriptionForLogs,
                            paramName, validationFailureMessage, valueStr);
                }
            } catch (NumberFormatException e) {
                logger.error("Invalid number format for {} ({}): '{}'. Error: {}", valueDescriptionForLogs, paramName,
                        valueStr, e.getMessage());
            }
        } else {
            logger.warn("{} ({}) not found or empty in web.xml.", valueDescriptionForLogs, paramName);
        }
    }

    private void loadAndStoreValidationPatterns(ServletContext context) {
        logger.info("Loading validation patterns from web.xml...");

        loadRegexPattern(context, "regex.username", USERNAME_REGEX_PATTERN, "username");
        loadRegexPattern(context, "regex.name", NAME_REGEX_PATTERN, "name/surname");

        loadIntegerValidation(context, "validation.password.minLength", PASSWORD_MIN_LENGTH, "password minimum length",
                val -> val > 0, "must be a positive integer");
        loadIntegerValidation(context, "validation.password.maxLength", PASSWORD_MAX_LENGTH, "password maximum length",
                val -> val > 0, "must be a positive integer");

        loadRegexPattern(context, "regex.standardText", STANDARD_TEXT_REGEX_PATTERN, "standard text");
        loadIntegerValidation(context, "validation.standardText.minLength", STANDARD_TEXT_MIN_LENGTH,
                "standard text minimum length", val -> val >= 0, "must be non-negative");
        loadIntegerValidation(context, "validation.standardText.maxLength", STANDARD_TEXT_MAX_LENGTH,
                "standard text maximum length", val -> val > 0, "must be a positive integer");

        loadRegexPattern(context, "regex.playlistName", PLAYLIST_NAME_REGEX_PATTERN, "playlist name");

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
