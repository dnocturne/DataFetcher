package me.dnoc;

import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.LongAdder;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;

import com.zaxxer.hikari.HikariConfig;

import me.dnoc.commands.DataFetcherCommand;
import me.dnoc.listeners.PlayerEventsListener;
import me.dnoc.listeners.VanishListener;

public final class DataFetcher extends JavaPlugin {

    private DatabaseManager databaseManager;
    private final Map<String, String> placeholders = new HashMap<>();
    // Used to track configuration state across plugin lifecycle
    @SuppressWarnings("unused") // Used in multiple methods for state tracking
    private boolean usingDefaultConfig = false;
    private final Metrics metrics = new Metrics();

    @Override
    public void onEnable() {
        saveDefaultConfig();

        // Add validation check before proceeding
        if (checkDefaultConfig() || !validateDatabaseConfig()) {
            usingDefaultConfig = true;
            getLogger().severe("Invalid database configuration");
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
                        new VanishListener(this),
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

    private boolean validateDatabaseConfig() {
        ConfigurationSection mysql = getConfig().getConfigurationSection("mysql");
        if (mysql == null) {
            return false;
        }

        String port = mysql.getString("port");
        try {
            assert port != null;
            int portNum = Integer.parseInt(port);
            if (portNum < 1 || portNum > 65535) {
                return false;
            }
        } catch (NumberFormatException e) {
            return false;
        }
        return !checkDefaultConfig();
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
            HikariConfig config = new HikariConfig();
            config.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + database);
            config.setUsername(username);
            config.setPassword(password);
            config.setInitializationFailTimeout(30000); // 30s initialization window
            config.setConnectionTimeout(30000); // More generous timeout

            this.databaseManager = new DatabaseManager(config);
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

    // Add metrics collection
    public static class Metrics {

        private final LongAdder queryCounter = new LongAdder();
        private final LongAdder errorCounter = new LongAdder();

        public void incrementQueries() {
            queryCounter.increment();
        }

        public void incrementErrors() {
            errorCounter.increment();
        }
    }

    public void executeUpdate(String query, Object... params) throws SQLException {
        try {
            metrics.incrementQueries();
            databaseManager.executeUpdate(query, params);
        } catch (SQLException e) {
            metrics.incrementErrors();
            throw e;
        }
    }
}
