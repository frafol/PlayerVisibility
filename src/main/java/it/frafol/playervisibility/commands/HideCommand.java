package it.frafol.playervisibility.commands;

import it.frafol.playervisibility.PlayerVisibility;
import it.frafol.playervisibility.enums.Config;
import it.frafol.playervisibility.objects.TextFile;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class HideCommand implements CommandExecutor, TabCompleter {

    private final PlayerVisibility plugin;

    public HideCommand(PlayerVisibility plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!sender.hasPermission(Config.PERMISSION.get(String.class))) {
            sender.sendMessage("§dThis server is using PlayerVisibility by frafol.");
            return true;
        }

        if (args.length != 1) {
            sender.sendMessage(Config.USAGE.color());
            return true;
        }

        String arg = args[0].toLowerCase();
        switch (arg) {
            case "reload":
                if (!sender.hasPermission(Config.RELOAD_PERMISSION.get(String.class))) {
                    sender.sendMessage(Config.NO_PERMISSION.color());
                    return true;
                }
                TextFile.reloadAll();
                sender.sendMessage(Config.RELOADED.color());
                break;

            case "hide":
                if (plugin.isHided()) {
                    sender.sendMessage(Config.ALREADY_HIDDEN.color());
                    return true;
                }
                plugin.hidePlayers();
                sender.sendMessage(Config.HIDDEN_MESSAGE.color());
                break;

            case "show":
                if (!plugin.isHided()) {
                    sender.sendMessage(Config.ALREADY_SHOWN.color());
                    return true;
                }
                plugin.showPlayers();
                sender.sendMessage(Config.SHOWN_MESSAGE.color());
                break;

            default:
                sender.sendMessage(Config.USAGE.color());
                break;
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> completions = new ArrayList<>();
            List<String> subCommands = Arrays.asList("reload", "hide", "show");
            String currentArg = args[0].toLowerCase();
            for (String sub : subCommands) {
                if (sub.startsWith(currentArg)) {
                    if (sub.equals("reload")) {
                        if (sender.hasPermission(Config.RELOAD_PERMISSION.get(String.class))) completions.add(sub);
                    } else {
                        completions.add(sub);
                    }
                }
            }
            return completions;
        }
        return null;
    }
}
