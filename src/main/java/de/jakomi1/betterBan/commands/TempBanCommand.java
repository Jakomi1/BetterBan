package de.jakomi1.betterBan.commands;

import de.jakomi1.betterBan.utils.BanUtils;
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

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static de.jakomi1.betterBan.BetterBan.chatPrefix;
import static de.jakomi1.betterBan.BetterBan.isAdmin;

public class TempBanCommand implements CommandExecutor, TabCompleter {

    @Override
    public boolean onCommand(@NotNull CommandSender sender,
                             @NotNull Command command,
                             @NotNull String label,
                             @NotNull String[] args) {
        // Permission check
        if (sender instanceof Player player && !isAdmin(player)) {
            sender.sendMessage(chatPrefix.append(Component.text("You don't have permission for this.", NamedTextColor.RED)));
            return true;
        }

        // /tempban <Name> <Duration> [Reason...]
        if (args.length < 2) {
            sender.sendMessage(chatPrefix.append(Component.text(
                    "Usage: /tempban <Name> <Duration> [Reason...]. Example: 10m, 2h, 1d",
                    NamedTextColor.RED
            )));
            return true;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
        UUID uuid = target.getUniqueId();

        // Check if player has ever joined
        if (!BanUtils.hasJoinedBefore(uuid)) {
            sender.sendMessage(chatPrefix.append(Component.text("This player has never joined the server.", NamedTextColor.RED)));
            return true;
        }

        // Check if player is already banned
        if (BanUtils.isBanned(uuid)) {
            sender.sendMessage(chatPrefix.append(Component.text(
                    (target.getName() != null ? target.getName() : uuid.toString()) + " is already banned!",
                    NamedTextColor.RED
            )));
            return true;
        }

        // Parse duration
        long delta;
        try {
            delta = parseDuration(args[1].toLowerCase());
        } catch (IllegalArgumentException e) {
            sender.sendMessage(chatPrefix.append(Component.text("Invalid time format. Example: 10m, 2h, 1d", NamedTextColor.RED)));
            return true;
        }

        long endTimestamp = System.currentTimeMillis() + delta;

        // Optional reason from args[2]...
        String reason = args.length >= 3 ? String.join(" ", Arrays.copyOfRange(args, 2, args.length)).trim() : null;

        // Save ban in DB
        BanUtils.tempBan(uuid, delta, reason);

        String name = Objects.toString(target.getName(), uuid.toString());
        String executor = sender instanceof Player ? sender.getName() : "the console";
        String remaining = BanUtils.formatDuration(delta);

        // Feedback to executor
        sender.sendMessage(chatPrefix.append(Component.text(name + " has been banned for " + remaining + ".", NamedTextColor.YELLOW)));
        if (reason != null && !reason.isBlank()) {
            sender.sendMessage(chatPrefix.append(Component.text("Reason: " + reason, NamedTextColor.GRAY)));
        }

        /* Discord-Log
        DiscordUtils.sendColoredMessage(
                name + " was banned by " + executor + " for " + remaining + "." + (reason != null ? "\nReason: " + reason : ""),
                0xFF0000
        );*/

        // Kick if online
        if (target.isOnline() && target.getPlayer() != null) {
            target.getPlayer().kick(BanUtils.getBanMessage(uuid));
        }

        return true;
    }

    private long parseDuration(String input) throws IllegalArgumentException {
        // allowed: <number><m|h|d>
        if (input.endsWith("m")) {
            return Long.parseLong(input.substring(0, input.length() - 1)) * 60_000L;
        } else if (input.endsWith("h")) {
            return Long.parseLong(input.substring(0, input.length() - 1)) * 60 * 60_000L;
        } else if (input.endsWith("d")) {
            return Long.parseLong(input.substring(0, input.length() - 1)) * 24 * 60 * 60_000L;
        } else {
            throw new IllegalArgumentException("Invalid time format");
        }
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender,
                                                @NotNull Command command,
                                                @NotNull String alias,
                                                @NotNull String[] args) {
        if (sender instanceof Player player && !isAdmin(player)) return List.of();

        if (args.length == 1) {
            // Suggest only players who joined before
            return Arrays.stream(Bukkit.getOfflinePlayers())
                    .filter(p -> BanUtils.hasJoinedBefore(p.getUniqueId()))
                    .map(OfflinePlayer::getName)
                    .filter(Objects::nonNull)
                    .filter(n -> n.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length == 2) {
            // Duration suggestions
            return Stream.of("10m", "30m", "1h", "2h", "1d")
                    .filter(opt -> opt.startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
        }

        return List.of();
    }
}
