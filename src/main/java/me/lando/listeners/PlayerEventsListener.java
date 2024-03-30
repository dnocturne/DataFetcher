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
	private final Logger logger; // Declare the logger

	// Modify the constructor to accept Logger
	public PlayerEventsListener(DatabaseManager databaseManager, DataFetcher plugin, Logger logger) {
		this.databaseManager = databaseManager;
		this.plugin = plugin;
		this.logger = logger; // Initialize the logger
	}


	@EventHandler
	public void onPlayerJoin(PlayerJoinEvent event) {
		Player player = event.getPlayer();
		String playerName = player.getName();

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
		String query = "UPDATE PlayerData SET online = FALSE WHERE username = ?;";
		databaseManager.executeUpdate(query, playerName); // Similarly, using playerName as a parameter
	}
}
