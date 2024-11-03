package me.dnoc;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class McNationDataFetcher extends PlaceholderExpansion {

	private final DataFetcher plugin;

	public McNationDataFetcher(DataFetcher plugin) {
		this.plugin = plugin;
	}

	@Override
	public boolean persist() {
		return true; // This makes sure the expansion doesn't unregister on reload.
	}

	@Override
	public boolean canRegister() {
		return true;
	}

	@Override
	public @NotNull String getAuthor() {
		return plugin.getPluginMeta().getAuthors().toString();
	}

	@Override
	public @NotNull String getIdentifier() {
		return "datafetcher";
	}

	@Override
	public @NotNull String getVersion() {
		return plugin.getPluginMeta().getVersion();
	}

	@Override
	public String onPlaceholderRequest(Player player, @NotNull String identifier) {
		if ("online".equalsIgnoreCase(identifier)) {
			return player.isOnline() ? "1" : "0";
		}
		return null; // If the placeholder is unknown
	}
}
