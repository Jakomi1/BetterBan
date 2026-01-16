package de.jakomi1.betterBan.commands;

import de.jakomi1.betterBan.utils.BanUtils;
import de.jakomi1.betterBan.utils.DiscordUtils;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.ChatColor;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import static de.jakomi1.betterBan.BetterBan.chatPrefix;
import static de.jakomi1.betterBan.BetterBan.isAdmin;

public class UnbanCommand implements CommandExecutor, TabCompleter {

    @Override
    public boolean onCommand(CommandSender sender,
                             Command command,
                             String label,
                             String[] args) {

        if (!(sender instanceof Player player) || isAdmin(player)) {

            if (args.length < 1) {
                sender.sendMessage(chatPrefix + ChatColor.RED + "Please provide a player name.");
                return true;
            }

            OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
            UUID uuid = target.getUniqueId();

            // Check if player is banned
            if (!BanUtils.isBanned(uuid)) {
                sender.sendMessage(chatPrefix + ChatColor.RED +
                        (target.getName() != null ? target.getName() : uuid.toString()) + " is not banned.");
                return true;
            }

            // Remove ban from DB
            BanUtils.unban(uuid);

            // Feedback
            sender.sendMessage(chatPrefix + ChatColor.GRAY +
                    (target.getName() != null ? target.getName() : uuid.toString()) + " has been unbanned.");

            String executor = sender instanceof Player ? sender.getName() : "the console";
            DiscordUtils.sendColoredMessage(
                    (target.getName() != null ? target.getName() : uuid.toString()) +
                            " was unbanned by " + executor + ".",
                    0x00FF00
            );

        } else {
            sender.sendMessage(chatPrefix + ChatColor.RED + "You don't have permission for this.");
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender,
                                      Command command,
                                      String alias,
                                      String[] args) {

        if (sender instanceof Player player && !isAdmin(player)) return List.of();

        if (args.length == 1) {
            return BanUtils.getAllBans().keySet().stream()
                    .map(Bukkit::getOfflinePlayer)
                    .map(OfflinePlayer::getName)
                    .filter(Objects::nonNull)
                    .filter(name -> name.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }

        return List.of();
    }
}
