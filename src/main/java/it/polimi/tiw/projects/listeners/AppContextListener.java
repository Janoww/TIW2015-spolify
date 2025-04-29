package it.polimi.tiw.projects.listeners;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;

import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Enumeration;

import com.mysql.cj.jdbc.AbandonedConnectionCleanupThread; // Import needed
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

@WebListener
public class AppContextListener implements ServletContextListener {

    private static final Logger logger = LoggerFactory.getLogger(AppContextListener.class);
    private HikariDataSource dataSource;

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
            // Optionally rethrow or handle context failure more gracefully
            throw new RuntimeException("Failed to initialize database connection pool", e);
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        // Close the DataSource when the application shuts down
        if (dataSource != null) {
            dataSource.close();
            logger.info("HikariCP DataSource closed.");
        }

        // * Tomcat gives a warning for memory leak for a thread created by jdbc
        // Deregister JDBC drivers loaded by this webapp's classloader
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        Enumeration<Driver> drivers = DriverManager.getDrivers();
        while (drivers.hasMoreElements()) {
            Driver driver = drivers.nextElement();
            if (driver.getClass().getClassLoader() == cl) {
                try {
                    logger.info("Deregistering JDBC driver: {}", driver);
                    DriverManager.deregisterDriver(driver);
                } catch (SQLException ex) {
                    logger.error("Error deregistering JDBC driver: {}", driver, ex);
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
