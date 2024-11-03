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
		return true;
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
		return "datafetcher";
	}

	@Override
	public @NotNull String getVersion() {
		return plugin.getDescription().getVersion();
	}

	@Override
	public String onPlaceholderRequest(Player player, @NotNull String identifier) {
		if (player == null) {
			return "";
		}

		if ("online".equalsIgnoreCase(identifier)) {
			return player.isOnline() ? "1" : "0";
		}

		if ("operator".equalsIgnoreCase(identifier)) {
			return player.isOp() ? "1" : "0";
		}

		return null;
	}
}