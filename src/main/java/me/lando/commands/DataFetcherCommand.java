package me.lando.commands;

import me.lando.DataFetcher;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class DataFetcherCommand implements CommandExecutor, TabCompleter {

	private final DataFetcher plugin;

	public DataFetcherCommand(DataFetcher plugin) {
		this.plugin = plugin;
	}

	@Override
	public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
		if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
			if (sender.hasPermission("datafetcher.reload")) {
				plugin.reloadConfig();
				String reloadMessage = plugin.getConfig().getString("messages.configurationReloaded", "<green>Configuration reloaded.");
				sender.sendMessage(MiniMessage.miniMessage().deserialize(reloadMessage));
			} else {
				String noPermissionMessage = plugin.getConfig().getString("messages.noPermissions", "<red>You do not have permission to execute this command.");
				sender.sendMessage(MiniMessage.miniMessage().deserialize(noPermissionMessage));
			}
			return true;
		}

		String usageMessage = plugin.getConfig().getString("messages.usage", "<red>Usage: /datafetcher reload");
		sender.sendMessage(MiniMessage.miniMessage().deserialize(usageMessage));
		return true;
	}

	@Override
	public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, String[] args) {
		List<String> completions = new ArrayList<>();
		if (args.length == 1) {
			completions.add("reload");
		}
		return completions;
	}
}
