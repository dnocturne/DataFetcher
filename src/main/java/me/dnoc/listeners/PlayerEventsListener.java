package me.dnoc.listeners;

import me.clip.placeholderapi.PlaceholderAPI;
import me.dnoc.DataFetcher;
import me.dnoc.DatabaseManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import com.earth2me.essentials.Essentials;

import java.sql.SQLException;
import java.util.Map;
import java.util.logging.Logger;

public class PlayerEventsListener implements Listener {

    private final DatabaseManager databaseManager;
    private final DataFetcher plugin;
    private static final Logger LOGGER = Logger.getLogger(PlayerEventsListener.class.getName());

    public PlayerEventsListener(DatabaseManager databaseManager, DataFetcher plugin) {
        this.databaseManager = databaseManager;
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        String playerName = player.getName();
        boolean isOp = player.isOp();

        // Check if player is vanished
        boolean isVanished = false;
        Essentials ess = (Essentials) Bukkit.getPluginManager().getPlugin("Essentials");
        if (ess != null) {
            isVanished = ess.getUser(player).isVanished();
        }

        try {
            // Insert player into PlayerData table if not exists
            if (!databaseManager.checkPlayerExists(playerName)) {
                String insertQuery = "INSERT INTO PlayerData (username, operator, online) VALUES (?, ?, ?);";
                databaseManager.executeUpdate(insertQuery, playerName, isOp, !isVanished);
            } else {
                // Update the online status based on vanish state
                String queryOnline = "UPDATE PlayerData SET online = ?, operator = ? WHERE username = ?";
                databaseManager.executeUpdate(queryOnline, !isVanished, isOp, playerName);
            }

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
                            LOGGER.warning(String.format("Placeholder %s could not be resolved for player %s", placeholder, playerName));
                        }
                    }
                }
            }

            LOGGER.info(String.format("Player %s data updated successfully. Operator status: %s, Vanished: %s", playerName, isOp, isVanished));
        } catch (SQLException e) {
            LOGGER.severe(String.format("Database error during player join for %s: %s", playerName, e.getMessage()));
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

            LOGGER.info(String.format("Player %s disconnected. Data updated successfully. Final operator status: %s", playerName, isOp));
        } catch (SQLException e) {
            LOGGER.severe(String.format("Database error during player quit for %s: %s", playerName, e.getMessage()));
        }
    }
}
