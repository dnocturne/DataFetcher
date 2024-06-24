package me.lando.listeners;

import me.clip.placeholderapi.PlaceholderAPI;
import me.lando.DataFetcher;
import me.lando.DatabaseManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

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

		// Insert player into PlayerData table if not exists
		if (!playerExists(playerName)) {
			String insertQuery = "INSERT INTO PlayerData (username) VALUES (?);";
			databaseManager.executeUpdate(insertQuery, playerName);
		}

		// Update the online status to TRUE when a player joins
		String queryOnline = "UPDATE PlayerData SET online = TRUE WHERE username = ?";
		databaseManager.executeUpdate(queryOnline, playerName);

		// Handle other dynamic placeholders
		plugin.getPlaceholders().forEach((column, placeholder) -> {
			if (!"online".equals(column)) { // Skip the 'online' column for dynamic placeholder handling
				String value = PlaceholderAPI.setPlaceholders(player, placeholder);
				if (!value.equals(placeholder)) { // Check if PlaceholderAPI replaced the placeholder
					String query = "UPDATE PlayerData SET " + column + " = ? WHERE username = ?";
					databaseManager.safeExecuteUpdate(column, query, value, playerName);
				} else {
					logger.warning("Placeholder " + placeholder + " could not be resolved for player " + playerName);
				}
			}
		});
	}

	@EventHandler
	public void onPlayerQuit(PlayerQuitEvent event) {
		String playerName = event.getPlayer().getName();
		// Update the online status to FALSE when a player leaves
		String query = "UPDATE PlayerData SET online = FALSE WHERE username = ?";
		databaseManager.executeUpdate(query, playerName);
	}

	private boolean playerExists(String username) {
		String query = "SELECT id FROM PlayerData WHERE username = ?";
		try {
			return databaseManager.playerExists(query, username);
		} catch (Exception e) {
			logger.severe("Could not check if player exists: " + e.getMessage());
			return false;
		}
	}
}