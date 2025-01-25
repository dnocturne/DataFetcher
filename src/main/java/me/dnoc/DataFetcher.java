package me.dnoc;

import me.dnoc.commands.DataFetcherCommand;
import me.dnoc.listeners.PlayerEventsListener;
import me.dnoc.listeners.VanishListener;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

public final class DataFetcher extends JavaPlugin {

    private DatabaseManager databaseManager;
    private final Map<String, String> placeholders = new HashMap<>();
    // Used to track configuration state across plugin lifecycle
    @SuppressWarnings("unused") // Used in multiple methods for state tracking
    private boolean usingDefaultConfig = false;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        // Check if using default configuration
        if (checkDefaultConfig()) {
            usingDefaultConfig = true;
            getLogger().severe("========================================");
            getLogger().severe("DATABASE CONNECTION NOT CONFIGURED!");
            getLogger().severe("Please update the MySQL connection details in config.yml");
            getLogger().severe("The plugin will not connect to the database until the default values are changed");
            getLogger().severe("========================================");
            return;
        }

        // If not using default config, proceed with normal initialization
        setupDatabaseConnection();

        if (databaseManager != null) {
            // Initialize database structure
            databaseManager.ensureTableExists();
            databaseManager.ensureColumnsForPlaceholders(placeholders);

            if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
                new McNationDataFetcher(this).register();
            }

            // Setup placeholders and whitelist
            setupPlaceholders();

            // Register events and commands
            getServer().getPluginManager().registerEvents(
                    new PlayerEventsListener(databaseManager, this),
                    this
            );

            if (Bukkit.getPluginManager().getPlugin("Essentials") != null) {
                getServer().getPluginManager().registerEvents(
                        new VanishListener(databaseManager),
                        this
                );
                getLogger().info("EssentialsX vanish support enabled!");
            }
        }

        Objects.requireNonNull(this.getCommand("datafetcher")).setExecutor(new DataFetcherCommand(this));
        Objects.requireNonNull(this.getCommand("datafetcher")).setTabCompleter(new DataFetcherCommand(this));
    }

    private boolean checkDefaultConfig() {
        String host = getConfig().getString("mysql.host", "");
        String username = getConfig().getString("mysql.username", "");
        String password = getConfig().getString("mysql.password", "");

        return "localhost".equals(host)
                && "user".equals(username)
                && "pass".equals(password);
    }

    private void setupPlaceholders() {
        placeholders.clear();
        Set<String> columnWhitelist = new HashSet<>();
        ConfigurationSection placeholdersSection = getConfig().getConfigurationSection("placeholders");
        if (placeholdersSection != null) {
            for (String key : placeholdersSection.getKeys(false)) {
                String placeholder = placeholdersSection.getString(key);
                placeholders.put(key, placeholder);
                columnWhitelist.add(key);
            }
        }
        columnWhitelist.add("online");
        columnWhitelist.add("operator");

        if (databaseManager != null) {
            databaseManager.setColumnWhitelist(columnWhitelist);
            databaseManager.ensureColumnsForPlaceholders(placeholders);
        }
    }

    private void setupDatabaseConnection() {
        try {
            String host = getConfig().getString("mysql.host");
            String port = getConfig().getString("mysql.port");
            String database = getConfig().getString("mysql.database");
            String username = getConfig().getString("mysql.username");
            String password = getConfig().getString("mysql.password");

            assert port != null;
            this.databaseManager = new DatabaseManager(host, Integer.parseInt(port), database, username, password);
            getLogger().info("Successfully connected to the database.");
        } catch (RuntimeException e) {
            getLogger().severe(String.format("Failed to establish database connection: %s", e.getMessage()));
            databaseManager = null;
        }
    }

    public void reloadPluginSettings() {
        reloadConfig();

        if (checkDefaultConfig()) {
            usingDefaultConfig = true;
            getLogger().severe("Cannot reload: MySQL connection details are still set to default values!");
            return;
        }

        usingDefaultConfig = false;
        setupDatabaseConnection();
        setupPlaceholders();
    }

    // Used by PlaceholderAPI integration
    @SuppressWarnings("unused")
    private DatabaseManager getDatabaseManager() {
        if (databaseManager == null) {
            getLogger().warning("Attempted to access database manager while it was not initialized!");
        }
        return databaseManager;
    }

    public Map<String, String> getPlaceholders() {
        return Collections.unmodifiableMap(placeholders);
    }
}
