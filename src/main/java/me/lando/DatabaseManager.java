package me.lando;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.*;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

public class DatabaseManager {

	private final HikariDataSource dataSource;
	private final Logger logger;

	// Whitelist for valid column names
	private Set<String> columnWhitelist;

	public DatabaseManager(String host, int port, String database, String username, String password, Logger logger) {
		this.logger = logger;

		// Configure HikariCP
		HikariConfig config = new HikariConfig();
		config.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + database + "?useSSL=false");
		config.setUsername(username);
		config.setPassword(password);

		// Optional: configure additional HikariCP settings here

		// Initialize HikariCP data source
		this.dataSource = new HikariDataSource(config);

		logger.info("DatabaseManager instance created with HikariCP.");
	}

	public Connection getConnection() throws SQLException {
		return dataSource.getConnection();
	}

	public void closePool() {
		if (dataSource != null && !dataSource.isClosed()) {
			dataSource.close();
		}
	}

	public void executeUpdate(String query, Object... params) {
		try (Connection conn = getConnection();
			 PreparedStatement ps = conn.prepareStatement(query)) {
			for (int i = 0; i < params.length; i++) {
				ps.setObject(i + 1, params[i]);
			}
			ps.executeUpdate();
		} catch (SQLException e) {
			logger.severe("SQL Exception: " + e.getMessage());
		}
	}

	// ... rest of your existing methods ...

	// Update methods below to use getConnection()

	public void safeExecuteUpdate(String columnName, String query, Object... params) {
		if (!isValidColumn(columnName)) {
			logger.warning("Attempted to use an invalid column name: " + columnName);
			return;
		}
		try (Connection conn = getConnection();
			 PreparedStatement ps = conn.prepareStatement(query)) {
			for (int i = 0; i < params.length; i++) {
				ps.setObject(i + 1, params[i]); // PrepareStatement parameters are 1-based.
			}
			ps.executeUpdate();
		} catch (SQLException e) {
			logger.severe("SQL Exception: " + e.getMessage());
		}
	}

	public void setColumnWhitelist(Set<String> columnWhitelist) {
		this.columnWhitelist = columnWhitelist;
		logger.info("Whitelist set in DatabaseManager: " + this.columnWhitelist);
	}

	public boolean isValidColumn(String columnName) {
		boolean valid = this.columnWhitelist != null && this.columnWhitelist.contains(columnName);
		logger.info("isValidColumn - Column: " + columnName + ", Valid: " + valid + ", Whitelist: " + columnWhitelist);
		return valid;
	}

	public void ensureColumnsForPlaceholders(Map<String, String> placeholders) {
		for (String column : placeholders.keySet()) {
			if (!isValidColumn(column)) {
				logger.warning("Column name '" + column + "' is not valid and will not be added.");
				continue;
			}
			try {
				if (!columnExists(column)) {
					String alterTableSql = "ALTER TABLE PlayerData ADD COLUMN " + column + " VARCHAR(255);";
					executeUpdate(alterTableSql);
				}
			} catch (SQLException e) {
				logger.severe("SQL Exception while ensuring column exists: " + e.getMessage());
			}
		}
	}

	public void ensureTableExists() {
		try {
			if (!tableExists()) {
				createPlayerDataTable();
			}
		} catch (SQLException e) {
			logger.severe("SQL Exception while ensuring the table exists: " + e.getMessage());
		}
	}

	private boolean tableExists() throws SQLException {
		try (Connection conn = getConnection();
			 ResultSet tables = conn.getMetaData().getTables(null, null, "PlayerData", null)) {
			return tables.next();
		}
	}

	private void createPlayerDataTable() throws SQLException {
		String createTableSql = "CREATE TABLE IF NOT EXISTS PlayerData (" +
				"id INT AUTO_INCREMENT PRIMARY KEY, " +
				"username VARCHAR(255) NOT NULL UNIQUE, " +
				"online BOOLEAN NOT NULL DEFAULT FALSE);";
		executeUpdate(createTableSql);
	}

	public boolean playerExists(String query, String username) throws SQLException {
		try (Connection conn = getConnection();
			 PreparedStatement ps = conn.prepareStatement(query)) {
			ps.setString(1, username);
			try (ResultSet rs = ps.executeQuery()) {
				return rs.next();
			}
		}
	}

	private boolean columnExists(String columnName) throws SQLException {
		try (Connection conn = getConnection();
			 ResultSet rs = conn.getMetaData().getColumns(null, null, "PlayerData", columnName)) {
			boolean exists = rs.next();
			rs.close();
			return exists;
		}
	}
}
