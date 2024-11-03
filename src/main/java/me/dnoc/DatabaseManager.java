package me.dnoc;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.*;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

public class DatabaseManager implements AutoCloseable {

	private final HikariDataSource dataSource;
	private final Logger logger;
	private Set<String> columnWhitelist;
	private static final int MAX_POOL_SIZE = 10;
	private static final int MIN_IDLE = 5;
	private static final int MAX_LIFETIME = 1800000; // 30 minutes
	private static final int CONNECTION_TIMEOUT = 5000; // 5 seconds

	public DatabaseManager(String host, int port, String database, String username, String password, Logger logger) {
		this.logger = logger;

		// Configure HikariCP with optimized settings
		HikariConfig config = new HikariConfig();
		config.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + database
				+ "?useSSL=false&serverTimezone=UTC&characterEncoding=utf8&autoReconnect=true");
		config.setUsername(username);
		config.setPassword(password);

		// Connection pool settings
		config.setMaximumPoolSize(MAX_POOL_SIZE);
		config.setMinimumIdle(MIN_IDLE);
		config.setMaxLifetime(MAX_LIFETIME);
		config.setConnectionTimeout(CONNECTION_TIMEOUT);
		config.setPoolName("DataFetcher-Pool");

		// Performance settings
		config.addDataSourceProperty("cachePrepStmts", "true");
		config.addDataSourceProperty("prepStmtCacheSize", "250");
		config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
		config.addDataSourceProperty("useServerPrepStmts", "true");

		// Additional connection properties for stability
		config.addDataSourceProperty("useUnicode", "true");
		config.addDataSourceProperty("allowPublicKeyRetrieval", "true");
		config.addDataSourceProperty("createDatabaseIfNotExist", "true");

		this.dataSource = new HikariDataSource(config);
		logger.info("DatabaseManager initialized with optimized connection pool settings.");
	}

	@Override
	public void close() {
		if (dataSource != null && !dataSource.isClosed()) {
			dataSource.close();
			logger.info("Database connection pool closed.");
		}
	}

	public Connection getConnection() throws SQLException {
		try {
			return dataSource.getConnection();
		} catch (SQLException e) {
			logger.severe("Failed to get database connection: " + e.getMessage());
			throw e;
		}
	}

	public void executeUpdate(String query, Object... params) throws SQLException {
		try (Connection connection = getConnection();
			 PreparedStatement ps = connection.prepareStatement(query)) {
			for (int i = 0; i < params.length; i++) {
				ps.setObject(i + 1, params[i]);
			}
			ps.executeUpdate();
		} catch (SQLException e) {
			logger.severe("Failed to execute update query: " + e.getMessage());
			throw e;
		}
	}

	public void setColumnWhitelist(Set<String> columnWhitelist) {
		this.columnWhitelist = Set.copyOf(columnWhitelist); // Create immutable copy
		logger.info("Column whitelist updated with " + this.columnWhitelist.size() + " entries");
	}

	private boolean isValidColumn(String columnName) {
		return columnWhitelist != null &&
				columnName != null &&
				columnWhitelist.contains(columnName);
	}

	public void ensureColumnsForPlaceholders(Map<String, String> placeholders) {
		try (Connection connection = getConnection()) {
			for (String column : placeholders.keySet()) {
				if (!isValidColumn(column)) {
					logger.warning("Skipping invalid column name: " + column);
					continue;
				}

				if (!columnExists(connection, column)) {
					String alterTableSql = "ALTER TABLE PlayerData ADD COLUMN " + column + " VARCHAR(255) DEFAULT NULL";
					try {
						executeUpdate(alterTableSql);
						logger.info("Added new column: " + column);
					} catch (SQLException e) {
						logger.severe("Failed to add column " + column + ": " + e.getMessage());
					}
				}
			}
		} catch (SQLException e) {
			logger.severe("Database error while ensuring columns: " + e.getMessage());
		}
	}

	public void ensureTableExists() {
		try {
			if (!tableExists()) {
				createPlayerDataTable();
				logger.info("Created PlayerData table");
			}
		} catch (SQLException e) {
			logger.severe("Failed to ensure table exists: " + e.getMessage());
		}
	}

	private boolean tableExists() throws SQLException {
		try (Connection connection = getConnection();
			 ResultSet rs = connection.getMetaData().getTables(null, null, "PlayerData", new String[]{"TABLE"})) {
			return rs.next();
		}
	}

	private void createPlayerDataTable() throws SQLException {
		String createTableSql = """
            CREATE TABLE IF NOT EXISTS PlayerData (
                id INT AUTO_INCREMENT PRIMARY KEY,
                username VARCHAR(255) NOT NULL UNIQUE,
                online BOOLEAN NOT NULL DEFAULT FALSE,
                operator BOOLEAN NOT NULL DEFAULT FALSE,
                last_seen TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                INDEX idx_username (username),
                INDEX idx_online (online),
                INDEX idx_operator (operator)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
            """;
		executeUpdate(createTableSql);
	}

	private boolean columnExists(Connection connection, String columnName) throws SQLException {
		try (ResultSet rs = connection.getMetaData().getColumns(null, null, "PlayerData", columnName)) {
			return rs.next();
		}
	}

	public boolean checkPlayerExists(String username) throws SQLException {
		String query = "SELECT 1 FROM PlayerData WHERE username = ? LIMIT 1";
		try (Connection connection = getConnection();
			 PreparedStatement ps = connection.prepareStatement(query)) {
			ps.setString(1, username);
			try (ResultSet rs = ps.executeQuery()) {
				return rs.next();
			}
		}
	}
}