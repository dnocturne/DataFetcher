package me.dnoc.listeners;

import me.clip.placeholderapi.PlaceholderAPI;
import me.dnoc.DataFetcher;
import me.dnoc.DatabaseManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.sql.SQLException;
import java.util.Map;
import java.util.logging.Logger;

public class PlayerEventsListener implements Listener {

	private final DatabaseManager databaseManager;
	private final DataFetcher plugin;
	private final Logger logger;

	public PlayerEventsListener(DatabaseManager databaseManager, DataFetcher plugin, Logger logger) {
		this.databaseManager = databaseManager;
		this.plugin = plugin;
		this.logger = logger;
	}

	@EventHandler
	public void onPlayerJoin(PlayerJoinEvent event) {
		Player player = event.getPlayer();
		String playerName = player.getName();
		boolean isOp = player.isOp();

		try {
			// Insert player into PlayerData table if not exists
			if (!databaseManager.checkPlayerExists(playerName)) {
				String insertQuery = "INSERT INTO PlayerData (username, operator) VALUES (?, ?);";
				databaseManager.executeUpdate(insertQuery, playerName, isOp);
			}

			// Update the online status and operator status when a player joins
			String queryOnline = "UPDATE PlayerData SET online = TRUE, operator = ? WHERE username = ?";
			databaseManager.executeUpdate(queryOnline, isOp, playerName);

			// Handle other dynamic placeholders
			Map<String, String> placeholders = plugin.getPlaceholders();
			if (!placeholders.isEmpty()) {
				for (Map.Entry<String, String> entry : placeholders.entrySet()) {
					String column = entry.getKey();
					String placeholder = entry.getValue();

					if (!"online".equals(column) && !"operator".equals(column)) {
						String value = PlaceholderAPI.setPlaceholders(player, placeholder);
						if (!placeholder.equals(value)) {
							String query = "UPDATE PlayerData SET " + column + " = ? WHERE username = ?";
							databaseManager.executeUpdate(query, value, playerName);
						} else {
							logger.warning("Placeholder " + placeholder + " could not be resolved for player " + playerName);
						}
					}
				}
			}

			logger.info("Player " + playerName + " data updated successfully. Operator status: " + isOp);
		} catch (SQLException e) {
			logger.severe("Database error during player join for " + playerName + ": " + e.getMessage());
		}
	}

	@EventHandler
	public void onPlayerQuit(PlayerQuitEvent event) {
		Player player = event.getPlayer();
		String playerName = player.getName();
		boolean isOp = player.isOp();

		try {
			// Update placeholders one last time before the player leaves
			Map<String, String> placeholders = plugin.getPlaceholders();
			if (!placeholders.isEmpty()) {
				for (Map.Entry<String, String> entry : placeholders.entrySet()) {
					String column = entry.getKey();
					String placeholder = entry.getValue();

					if (!"online".equals(column) && !"operator".equals(column)) {
						String value = PlaceholderAPI.setPlaceholders(player, placeholder);
						if (!placeholder.equals(value)) {
							String query = "UPDATE PlayerData SET " + column + " = ? WHERE username = ?";
							databaseManager.executeUpdate(query, value, playerName);
						}
					}
				}
			}

			// Update the online status and operator status when a player leaves
			String query = "UPDATE PlayerData SET online = FALSE, operator = ? WHERE username = ?";
			databaseManager.executeUpdate(query, isOp, playerName);

			logger.info("Player " + playerName + " disconnected. Data updated successfully. Final operator status: " + isOp);
		} catch (SQLException e) {
			logger.severe("Database error during player quit for " + playerName + ": " + e.getMessage());
		}
	}
}