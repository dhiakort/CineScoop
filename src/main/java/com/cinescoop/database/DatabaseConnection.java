package com.cinescoop.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

/**
 * DatabaseConnection — Thread-safe Singleton using HikariCP connection pool.
 *
 * Usage:
 *   Connection conn = DatabaseConnection.getInstance().getConnection();
 *   DatabaseConnection.getInstance().close(conn);
 */
public class DatabaseConnection {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseConnection.class);

    // Config file location inside src/main/resources/
    private static final String CONFIG_FILE = "db.properties";

    // Singleton instance (volatile for thread-safe double-checked locking)
    private static volatile DatabaseConnection instance;

    // HikariCP connection pool
    private HikariDataSource dataSource;

    // ─────────────────────────────────────────────────────────────
    //  PRIVATE CONSTRUCTOR — loads config and initialises the pool
    // ─────────────────────────────────────────────────────────────
    private DatabaseConnection() {
        try {
            Properties props = loadProperties();
            initPool(props);
            logger.info("✅ Database connection pool initialised successfully.");
        } catch (Exception e) {
            logger.error("❌ Failed to initialise database connection pool: {}", e.getMessage());
            throw new RuntimeException("Cannot initialise DatabaseConnection", e);
        }
    }

    // ─────────────────────────────────────────────────────────────
    //  SINGLETON — double-checked locking (thread-safe, lazy)
    // ─────────────────────────────────────────────────────────────
    public static DatabaseConnection getInstance() {
        if (instance == null) {
            synchronized (DatabaseConnection.class) {
                if (instance == null) {
                    instance = new DatabaseConnection();
                }
            }
        }
        return instance;
    }

    // ─────────────────────────────────────────────────────────────
    //  LOAD PROPERTIES from db.properties
    // ─────────────────────────────────────────────────────────────
    private Properties loadProperties() {
        Properties props = new Properties();

        try (InputStream in = getClass().getClassLoader().getResourceAsStream(CONFIG_FILE)) {
            if (in == null) {
                logger.warn("⚠️  {} not found — using hardcoded defaults.", CONFIG_FILE);
                return getDefaultProperties();
            }
            props.load(in);
            logger.info("📄 Loaded database config from {}", CONFIG_FILE);

        } catch (IOException e) {
            logger.warn("⚠️  Could not read {} — using defaults. Cause: {}", CONFIG_FILE, e.getMessage());
            return getDefaultProperties();
        }

        return props;
    }

    // Fallback hardcoded config (used if db.properties is absent)
    private Properties getDefaultProperties() {
        Properties props = new Properties();
        props.setProperty("db.url",      "jdbc:mysql://localhost:3306/cinescoop?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true&characterEncoding=UTF-8");
        props.setProperty("db.username", "root");
        props.setProperty("db.password", "");
        props.setProperty("db.pool.size",        "10");
        props.setProperty("db.pool.minIdle",     "2");
        props.setProperty("db.pool.idleTimeout", "300000");
        props.setProperty("db.pool.maxLifetime", "1800000");
        props.setProperty("db.pool.timeout",     "30000");
        return props;
    }

    // ─────────────────────────────────────────────────────────────
    //  INIT HIKARICP POOL
    // ─────────────────────────────────────────────────────────────
    private void initPool(Properties props) {
        HikariConfig config = new HikariConfig();

        // Core JDBC settings
        config.setJdbcUrl(props.getProperty("db.url",
                "jdbc:mysql://localhost:3306/cinescoop?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true&characterEncoding=UTF-8"));
        config.setUsername(props.getProperty("db.username", "root"));
        config.setPassword(props.getProperty("db.password", ""));
        config.setDriverClassName("com.mysql.cj.jdbc.Driver");

        // Pool tuning
        config.setMaximumPoolSize(
                Integer.parseInt(props.getProperty("db.pool.size", "10")));
        config.setMinimumIdle(
                Integer.parseInt(props.getProperty("db.pool.minIdle", "2")));
        config.setIdleTimeout(
                Long.parseLong(props.getProperty("db.pool.idleTimeout", "300000")));
        config.setMaxLifetime(
                Long.parseLong(props.getProperty("db.pool.maxLifetime", "1800000")));
        config.setConnectionTimeout(
                Long.parseLong(props.getProperty("db.pool.timeout", "30000")));

        // Pool identity
        config.setPoolName("CineScoopPool");

        // Keep-alive query (prevents stale connections)
        config.setConnectionTestQuery("SELECT 1");
        config.setKeepaliveTime(60_000);

        // MySQL-specific optimisations
        config.addDataSourceProperty("cachePrepStmts",          "true");
        config.addDataSourceProperty("prepStmtCacheSize",        "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit",    "2048");
        config.addDataSourceProperty("useServerPrepStmts",       "true");
        config.addDataSourceProperty("useLocalSessionState",     "true");
        config.addDataSourceProperty("rewriteBatchedStatements", "true");
        config.addDataSourceProperty("cacheResultSetMetadata",   "true");
        config.addDataSourceProperty("cacheServerConfiguration", "true");
        config.addDataSourceProperty("elideSetAutoCommits",      "true");
        config.addDataSourceProperty("maintainTimeStats",        "false");

        dataSource = new HikariDataSource(config);
    }

    // ─────────────────────────────────────────────────────────────
    //  PUBLIC API
    // ─────────────────────────────────────────────────────────────

    /**
     * Borrow a connection from the pool.
     * Always call close(conn) or use try-with-resources when done.
     */
    public Connection getConnection() throws SQLException {
        if (dataSource == null || dataSource.isClosed()) {
            throw new SQLException("Connection pool is not initialised or has been shut down.");
        }
        Connection conn = dataSource.getConnection();
        logger.debug("🔗 Connection acquired from pool.");
        return conn;
    }

    /**
     * Return a connection to the pool (does NOT close the physical connection).
     */
    public void close(Connection connection) {
        if (connection != null) {
            try {
                connection.close();   // returns to pool, not physical close
                logger.debug("🔓 Connection returned to pool.");
            } catch (SQLException e) {
                logger.warn("⚠️  Error returning connection to pool: {}", e.getMessage());
            }
        }
    }

    /**
     * Test whether the pool can reach the database.
     * Call this on application startup to fail fast.
     */
    public boolean testConnection() {
        try (Connection conn = getConnection()) {
            boolean valid = conn.isValid(5);   // 5-second timeout
            if (valid) {
                logger.info("✅ Database connectivity test passed.");
            } else {
                logger.error("❌ Database connectivity test FAILED.");
            }
            return valid;
        } catch (SQLException e) {
            logger.error("❌ Cannot reach MySQL: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Return pool statistics (active/idle/total connections).
     */
    public String getPoolStats() {
        if (dataSource == null) return "Pool not initialised";
        return String.format(
                "Pool[%s] — active: %d | idle: %d | total: %d | waiting: %d",
                dataSource.getPoolName(),
                dataSource.getHikariPoolMXBean().getActiveConnections(),
                dataSource.getHikariPoolMXBean().getIdleConnections(),
                dataSource.getHikariPoolMXBean().getTotalConnections(),
                dataSource.getHikariPoolMXBean().getThreadsAwaitingConnection()
        );
    }

    /**
     * Shut down the pool gracefully.
     * Call once on application exit (e.g. Platform.exit() hook).
     */
    public void shutdown() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            instance = null;
            logger.info("🛑 Connection pool shut down.");
        }
    }

    /**
     * Convenience: execute a quick query and return whether it succeeded.
     * Useful for INSERT / UPDATE / DELETE without needing to manage Connection manually.
     */
    public boolean executeUpdate(String sql, Object... params) {
        try (Connection conn = getConnection();
             var stmt = conn.prepareStatement(sql)) {

            for (int i = 0; i < params.length; i++) {
                stmt.setObject(i + 1, params[i]);
            }
            int rows = stmt.executeUpdate();
            logger.debug("✏️  executeUpdate affected {} row(s) — SQL: {}", rows, sql);
            return rows > 0;

        } catch (SQLException e) {
            logger.error("❌ executeUpdate failed: {} | SQL: {}", e.getMessage(), sql);
            return false;
        }
    }
}