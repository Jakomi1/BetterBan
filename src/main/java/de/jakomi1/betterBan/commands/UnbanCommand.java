package de.jakomi1.betterBan.commands;

import de.jakomi1.betterBan.utils.BanUtils;
import de.jakomi1.betterBan.utils.DiscordUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import static de.jakomi1.betterBan.BetterBan.chatPrefix;
import static de.jakomi1.betterBan.BetterBan.isAdmin;

public class UnbanCommand implements CommandExecutor, TabCompleter {

    @Override
    public boolean onCommand(@NotNull CommandSender sender,
                             @NotNull Command command,
                             @NotNull String label,
                             @NotNull String[] args) {

        if (!(sender instanceof Player player) || isAdmin(player)) {
            if (args.length < 1) {
                sender.sendMessage(chatPrefix.append(Component.text("Please provide a player name.", NamedTextColor.RED)));
                return true;
            }

            OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
            UUID uuid = target.getUniqueId();

            // Check if player is banned
            if (!BanUtils.isBanned(uuid)) {
                sender.sendMessage(chatPrefix.append(Component.text(target.getName() + " is not banned.", NamedTextColor.RED)));
                return true;
            }

            // Remove ban from DB
            BanUtils.unban(uuid);

            sender.sendMessage(chatPrefix.append(Component.text(target.getName() + " has been unbanned.", NamedTextColor.GRAY)));
            String executor = sender instanceof Player ? sender.getName() : "the console";
            DiscordUtils.sendColoredMessage(target.getName() + " was unbanned by " + executor + ".", 65280);

        } else {
            sender.sendMessage(chatPrefix.append(Component.text("You don't have permission for this.", NamedTextColor.RED)));
        }

        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender,
                                                @NotNull Command command,
                                                @NotNull String alias,
                                                @NotNull String[] args) {

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
