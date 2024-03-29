package me.lando;

import java.sql.*;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

public class DatabaseManager {

	private static Connection connection;
	private final String host, database, username, password;
	private final int port;
	private final Logger logger;

	// Whitelist for valid column names
	private Set<String> columnWhitelist;

	public DatabaseManager(String host, int port, String database, String username, String password, Logger logger) {
		this.host = host;
		this.port = port;
		this.database = database;
		this.username = username;
		this.password = password;
		this.logger = logger;
		logger.info("DatabaseManager instance created.");
	}

	public boolean openConnection() {
		try {
			if (connection != null && !connection.isClosed()) {
				return true;
			}
			synchronized (this) {
				if (connection != null && !connection.isClosed()) {
					return true;
				}
				Class.forName("com.mysql.cj.jdbc.Driver");
				connection = DriverManager.getConnection("jdbc:mysql://" + this.host + ":" + this.port + "/" + this.database + "?useSSL=false", this.username, this.password);
			}
			logger.info("Successfully connected to the database.");
			return true;
		} catch (SQLException | ClassNotFoundException e) {
			logger.severe("SQL Exception: " + e.getMessage());
			return false;
		}
	}

	public void closeConnection() {
		try {
			if (connection != null && !connection.isClosed()) {
				connection.close();
			}
		} catch (SQLException e) {
			logger.severe("SQL Exception: " + e.getMessage());
		}
	}

	public void executeUpdate(String query, Object... params) {
		try (PreparedStatement ps = connection.prepareStatement(query)) {
			for (int i = 0; i < params.length; i++) {
				ps.setObject(i + 1, params[i]);
			}
			ps.executeUpdate();
		} catch (SQLException e) {
			logger.severe("SQL Exception: " + e.getMessage());
		}
	}

	// Add to your DatabaseManager.java
	public void safeExecuteUpdate(String columnName, String query, Object... params) {
		if (!isValidColumn(columnName)) {
			logger.warning("Attempted to use an invalid column name: " + columnName);
			return;
		}
		try (PreparedStatement ps = connection.prepareStatement(query)) {
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
				// This method needs implementation to check column existence before attempting to add
				if (!columnExists(column)) {
					String alterTableSql = "ALTER TABLE PlayerData ADD COLUMN " + column + " VARCHAR(255);";
					executeUpdate(alterTableSql);
				}
			} catch (SQLException e) {
				logger.severe("SQL Exception while ensuring column exists: " + e.getMessage());
			}
		}
	}

	private boolean columnExists(String columnName) throws SQLException {
		ResultSet rs = connection.getMetaData().getColumns(null, null, "PlayerData", columnName);
		boolean exists = rs.next();
		rs.close();
		return exists;
	}
}
