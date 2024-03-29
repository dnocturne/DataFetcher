package me.lando;

import me.lando.listeners.PlayerEventsListener;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;

public final class datafetcher extends JavaPlugin {

	// Declare the DatabaseManager as a class variable
	private DatabaseManager databaseManager;

	@Override
	public void onEnable() {
		// Ensure config.yml exists; if not, create one with default settings.
		saveDefaultConfig(); // This method creates a config.yml if one doesn't exist, using the one from your resources folder as a template.

		// You can now attempt to establish a connection to the MySQL database using the credentials stored in config.yml
		setupDatabaseConnection();

		// Register PlaceholderAPI expansion
		if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
			new McNationDataFetcher(this).register();
		}

		// Register the player events listener
		getServer().getPluginManager().registerEvents(new PlayerEventsListener(databaseManager), this);

		// Load placeholders from config and update database schema
		Map<String, String> placeholders = new HashMap<>();
		if (getConfig().isConfigurationSection("placeholders")) {
			ConfigurationSection placeholdersSection = getConfig().getConfigurationSection("placeholders");
			assert placeholdersSection != null;
			for (String key : placeholdersSection.getKeys(false)) {
				placeholders.put(key, placeholdersSection.getString(key));
			}
			databaseManager.ensureColumnsForPlaceholders(placeholders);
		}
	}

	@Override
	public void onDisable() {
		// Close database connection when plugin is disabled
		if (databaseManager != null) {
			databaseManager.closeConnection();
		}
	}

	private void setupDatabaseConnection() {
		String host = getConfig().getString("mysql.host");
		String port = getConfig().getString("mysql.port");
		String database = getConfig().getString("mysql.database");
		String username = getConfig().getString("mysql.username");
		String password = getConfig().getString("mysql.password");

		// Instantiate the DatabaseManager with database connection details
		assert port != null;
		databaseManager = new DatabaseManager(host, Integer.parseInt(port), database, username, password, this.getLogger());


		// Attempt to open the database connection
		if (databaseManager.openConnection()) {
			getLogger().info("Successfully connected to the database.");
		} else {
			getLogger().severe("Could not connect to the database.");
		}
	}
}
