package it.polimi.tiw.projects.listeners;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

@WebListener
public class AppContextListener implements ServletContextListener {

    private HikariDataSource dataSource;

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        ServletContext context = sce.getServletContext();

        // Configure HikariCP
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(context.getInitParameter("dbUrl"));
        config.setUsername(context.getInitParameter("dbUser"));
        config.setPassword(context.getInitParameter("dbPassword"));
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
        dataSource = new HikariDataSource(config);

        // Store the DataSource in the ServletContext
        context.setAttribute("dataSource", dataSource);

        System.out.println("HikariCP DataSource initialized and added to ServletContext.");
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        // Close the DataSource when the application shuts down
        if (dataSource != null) {
            dataSource.close();
            System.out.println("HikariCP DataSource closed.");
        }
    }
}
