package me.dnoc.listeners;

import me.clip.placeholderapi.PlaceholderAPI;
import me.dnoc.DataFetcher;
import me.dnoc.DatabaseManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class PlayerEventsListener implements Listener {

	private final DatabaseManager databaseManager;
	private final DataFetcher plugin;
	private final Logger logger;
	private static final int UPDATE_DELAY_TICKS = 20; // 1 second delay

	public PlayerEventsListener(DatabaseManager databaseManager, DataFetcher plugin, Logger logger) {
		this.databaseManager = databaseManager;
		this.plugin = plugin;
		this.logger = logger;
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onPlayerJoin(PlayerJoinEvent event) {
		Player player = event.getPlayer();
		String playerName = player.getName();

		// Run database operations async
		new BukkitRunnable() {
			@Override
			public void run() {
				try {
					handlePlayerJoin(player);
				} catch (SQLException e) {
					logger.log(Level.SEVERE, "Failed to handle player join for " + playerName, e);
				}
			}
		}.runTaskAsynchronously(plugin);
	}

	private void handlePlayerJoin(Player player) throws SQLException {
		String playerName = player.getName();

		// First, ensure player exists in database
		if (!playerExists(playerName)) {
			String insertQuery = "INSERT INTO PlayerData (username, online) VALUES (?, TRUE)";
			databaseManager.executeUpdate(insertQuery, playerName);
			logger.info("New player " + playerName + " added to database");
		} else {
			// Update online status
			String queryOnline = "UPDATE PlayerData SET online = TRUE WHERE username = ?";
			databaseManager.executeUpdate(queryOnline, playerName);
		}

		// Handle placeholders update
		updatePlayerPlaceholders(player);
	}

	private void updatePlayerPlaceholders(Player player) throws SQLException {
		Map<String, String> placeholders = plugin.getPlaceholders();
		if (placeholders.isEmpty()) {
			return;
		}

		Map<String, String> updates = new HashMap<>();
		for (Map.Entry<String, String> entry : placeholders.entrySet()) {
			String column = entry.getKey();
			String placeholder = entry.getValue();

			if ("online".equals(column)) {
				continue;
			}

			String value = PlaceholderAPI.setPlaceholders(player, placeholder);
			if (!value.equals(placeholder)) {
				updates.put(column, value);
			} else {
				logger.warning("Placeholder " + placeholder + " could not be resolved for " + player.getName());
			}
		}

		// Batch update all placeholders
		if (!updates.isEmpty()) {
			StringBuilder query = new StringBuilder("UPDATE PlayerData SET ");
			String[] params = new String[updates.size() + 1];
			int paramIndex = 0;

			for (Map.Entry<String, String> entry : updates.entrySet()) {
				if (paramIndex > 0) {
					query.append(", ");
				}
				query.append(entry.getKey()).append(" = ?");
				params[paramIndex++] = entry.getValue();
			}
			query.append(" WHERE username = ?");
			params[paramIndex] = player.getName();

			databaseManager.safeExecuteUpdate(updates.keySet().iterator().next(), query.toString(), (Object[]) params);
		}
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onPlayerQuit(PlayerQuitEvent event) {
		Player player = event.getPlayer();
		String playerName = player.getName();

		// Run database operations async
		new BukkitRunnable() {
			@Override
			public void run() {
				try {
					String query = "UPDATE PlayerData SET online = FALSE WHERE username = ?";
					databaseManager.executeUpdate(query, playerName);
				} catch (SQLException e) {
					logger.log(Level.SEVERE, "Failed to update player quit status for " + playerName, e);
				}
			}
		}.runTaskAsynchronously(plugin);
	}

	private boolean playerExists(String username) throws SQLException {
		String query = "SELECT 1 FROM PlayerData WHERE username = ? LIMIT 1";
		return databaseManager.playerExists(query, username);
	}
}