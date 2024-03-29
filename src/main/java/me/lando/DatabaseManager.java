package me.lando;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Map;
import java.util.logging.Logger;

public class DatabaseManager {

	private static Connection connection;
	private final String host, database, username, password;
	private final int port;

	private final Logger logger;

	public DatabaseManager(String host, int port, String database, String username, String password, Logger logger) {
		this.host = host;
		this.port = port;
		this.database = database;
		this.username = username;
		this.password = password;
		this.logger = logger;
	}

	/**
	 * Opens a connection to the MySQL database.
	 */
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
				connection = DriverManager.getConnection("jdbc:mysql://" +
								this.host + ":" + this.port + "/" + this.database + "?useSSL=false",
						this.username, this.password);
			}
			if (openConnection()) {
				initializeDatabase();
			}
			return true;
		} catch (SQLException | ClassNotFoundException e) {
			logger.severe("SQL Exception: " + e.getMessage());
			return false;
		}
	}

	/**
	 * Closes the current MySQL connection.
	 */
	public void closeConnection() {
		try {
			if (connection != null && !connection.isClosed()) {
				connection.close();
			}
		} catch (SQLException e) {
			logger.severe("SQL Exception: " + e.getMessage());
		}
	}

	/**
	 * Executes a SQL query that does not return a ResultSet, such as an UPDATE or INSERT.
	 *
	 * @param query The SQL query to execute.
	 */
	public void executeUpdate(String query, Object... params) {
		try (PreparedStatement ps = connection.prepareStatement(query)) {
			for (int i = 0; i < params.length; i++) {
				ps.setObject(i + 1, params[i]); // PreparedStatement parameters are 1-based
			}
			ps.executeUpdate();
		} catch (SQLException e) {
			logger.severe("SQL Exception: " + e.getMessage());
			// Optionally log more details or the stack trace as fine/debug level
			logger.fine("Stack Trace: " + Arrays.toString(e.getStackTrace()));
		}
	}

	public void initializeDatabase() {
		// Create the PlayerData table if it doesn't exist
		String createTableSQL = "CREATE TABLE IF NOT EXISTS PlayerData (" +
				"id INT AUTO_INCREMENT PRIMARY KEY, " +
				"username VARCHAR(255) NOT NULL UNIQUE, " +
				"online BOOLEAN NOT NULL" +
				");";
		executeUpdate(createTableSQL);
	}

	public void ensureColumnsForPlaceholders(Map<String, String> placeholders) {
		for (String column : placeholders.keySet()) {
			String alterTableSql = "ALTER TABLE PlayerData ADD COLUMN IF NOT EXISTS " + column + " VARCHAR(255);";
			executeUpdate(alterTableSql);
		}
	}
}
