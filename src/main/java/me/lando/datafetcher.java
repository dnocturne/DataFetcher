package me.lando;

import me.lando.listeners.PlayerEventsListener;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class datafetcher extends JavaPlugin {

	// Declare the DatabaseManager as a class variable
	private DatabaseManager databaseManager;

	// Declare the placeholders map
	private final Map<String, String> placeholders = new HashMap<>();

	@Override
	public void onEnable() {
		saveDefaultConfig();
		setupDatabaseConnection();

		if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
			new McNationDataFetcher(this).register();
		}

		placeholders.clear(); // Clear previous entries
		Set<String> columnWhitelist = new HashSet<>();
		ConfigurationSection placeholdersSection = getConfig().getConfigurationSection("placeholders");
		if (placeholdersSection != null) {
			for (String key : placeholdersSection.getKeys(false)) {
				String placeholder = placeholdersSection.getString(key);
				placeholders.put(key, placeholder);
				columnWhitelist.add(key); // Populate the whitelist
			}
		}

		// Set the whitelist before attempting any database modifications
		databaseManager.setColumnWhitelist(columnWhitelist);
		getLogger().info("Column whitelist set.");

		// Now, ensure columns for placeholders
		databaseManager.ensureColumnsForPlaceholders(placeholders);

		getServer().getPluginManager().registerEvents(new PlayerEventsListener(databaseManager, this, this.getLogger()), this);
	}

	public Map<String, String> getPlaceholders() {
		return placeholders;
	}

	private void setupDatabaseConnection() {
		String host = getConfig().getString("mysql.host");
		String port = getConfig().getString("mysql.port");
		String database = getConfig().getString("mysql.database");
		String username = getConfig().getString("mysql.username");
		String password = getConfig().getString("mysql.password");

		// Instantiate the DatabaseManager with database connection details
		assert port != null;
		this.databaseManager = new DatabaseManager(host, Integer.parseInt(port), database, username, password, this.getLogger());


		// Attempt to open the database connection
		if (databaseManager.openConnection()) {
			getLogger().info("Successfully connected to the database.");
		} else {
			getLogger().severe("Could not connect to the database.");
		}
	}
}
