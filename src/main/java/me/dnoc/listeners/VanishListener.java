package me.dnoc.listeners;

import java.sql.SQLException;
import java.util.logging.Logger;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import me.dnoc.DataFetcher;
import net.ess3.api.IUser;
import net.ess3.api.events.VanishStatusChangeEvent;

public class VanishListener implements Listener {

    private final DataFetcher plugin;
    private static final Logger LOGGER = Logger.getLogger(VanishListener.class.getName());

    public VanishListener(DataFetcher plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onVanishStatusChange(VanishStatusChangeEvent event) {
        IUser user = event.getAffected();
        String playerName = user.getName();
        try {
            // If player is vanishing, set them as offline in the database
            // If they're becoming visible, set them as online
            plugin.executeUpdate("UPDATE players SET online = ? WHERE username = ?",
                    !event.getValue() ? 1 : 0, playerName);
            LOGGER.info(String.format("Updated vanish status for %s (Vanished: %s)", playerName, event.getValue()));
        } catch (SQLException e) {
            LOGGER.severe(String.format("Failed to update vanish status for %s: %s", playerName, e.getMessage()));
        }
    }
}
