package me.lando.listeners;

import me.lando.DatabaseManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerEventsListener implements Listener {

	private final DatabaseManager databaseManager;

	public PlayerEventsListener(DatabaseManager databaseManager) {
		this.databaseManager = databaseManager;
	}

	@EventHandler
	public void onPlayerJoin(PlayerJoinEvent event) {
		String playerName = event.getPlayer().getName();
		// The query now uses placeholders for parameters
		String query = "INSERT INTO PlayerData (username, online) VALUES (?, TRUE) ON DUPLICATE KEY UPDATE online = TRUE;";
		databaseManager.executeUpdate(query, playerName); // playerName is used as a parameter
	}

	@EventHandler
	public void onPlayerQuit(PlayerQuitEvent event) {
		String playerName = event.getPlayer().getName();
		String query = "UPDATE PlayerData SET online = FALSE WHERE username = ?;";
		databaseManager.executeUpdate(query, playerName); // Similarly, using playerName as a parameter
	}
}
