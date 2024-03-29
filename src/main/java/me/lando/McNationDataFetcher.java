package me.lando;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class McNationDataFetcher extends PlaceholderExpansion {

	private final datafetcher plugin;

	public McNationDataFetcher(datafetcher plugin) {
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
		return plugin.getDescription().getAuthors().toString();
	}

	@Override
	public @NotNull String getIdentifier() {
		return "datafetcher"; // This will be your placeholder identifier.
	}

	@Override
	public @NotNull String getVersion() {
		return plugin.getDescription().getVersion();
	}

	@Override
	public String onPlaceholderRequest(Player player, @NotNull String identifier) {
		// Here you respond to placeholder requests. For example:
		if ("online".equalsIgnoreCase(identifier)) {
			return player.isOnline() ? "1" : "0"; // Just an example, you'd fetch actual data related to the player here.
		}
		return null; // If the placeholder is unknown
	}
}
