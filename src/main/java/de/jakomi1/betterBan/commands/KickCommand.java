package de.jakomi1.betterBan.commands;

import de.jakomi1.betterBan.utils.DiscordUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static de.jakomi1.betterBan.BetterBan.chatPrefix;
import static de.jakomi1.betterBan.BetterBan.isAdmin;

public class KickCommand implements CommandExecutor, TabCompleter {

    @Override
    public boolean onCommand(CommandSender sender,
                             Command command,
                             String label,
                             String[] args) {

        // Permission check: players must be moderator; console allowed
        if (sender instanceof Player player && !isAdmin(player)) {
            sender.sendMessage(chatPrefix + ChatColor.RED + "You don't have permission for this.");
            return true;
        }

        // Check: /kick <Name> [Reason]
        if (args.length < 1) {
            sender.sendMessage(chatPrefix + ChatColor.RED + "Usage: /kick <Name> [Reason]");
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            sender.sendMessage(chatPrefix + ChatColor.RED + "This player is not online.");
            return true;
        }

        // Prevent self-kick
        if (sender instanceof Player && ((Player) sender).getUniqueId().equals(target.getUniqueId())) {
            sender.sendMessage(chatPrefix + ChatColor.RED + "You cannot kick yourself.");
            return true;
        }

        // Build reason
        String reason = null;
        if (args.length >= 2) {
            reason = String.join(" ", Arrays.copyOfRange(args, 1, args.length)).trim();
            if (reason.isBlank()) reason = null;
        }

        // Kick message for player
        String kickMessage = chatPrefix + ChatColor.RED + "You have been kicked from the server!";
        if (reason != null) {
            kickMessage += ChatColor.GRAY + "\nReason: " + reason;
        }

        // Executor name (console friendly)
        String executor = sender instanceof Player ? sender.getName() : "the console";

        // Discord notification
        String discordMsg = target.getName() + " was kicked by " + executor
                + (reason != null ? "\nReason: " + reason : "");
        DiscordUtils.sendColoredMessage(discordMsg, 16753920);

        // Feedback to executor
        String executorFeedback = chatPrefix + ChatColor.YELLOW + target.getName()
                + " was kicked by " + executor
                + (reason != null ? " Reason: " + reason : "");
        sender.sendMessage(executorFeedback);

        // Execute kick
        target.kickPlayer(kickMessage);

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender,
                                      Command command,
                                      String alias,
                                      String[] args) {

        if (sender instanceof Player player && !isAdmin(player)) {
            return List.of();
        }

        if (args.length == 1) {
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }

        return List.of();
    }
}
