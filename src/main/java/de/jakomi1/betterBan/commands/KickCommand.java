package de.jakomi1.betterBan.commands;

import de.jakomi1.betterBan.utils.DiscordUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static de.jakomi1.betterBan.BetterBan.chatPrefix;
import static de.jakomi1.betterBan.BetterBan.isAdmin;

public class KickCommand implements CommandExecutor, TabCompleter {

    @Override
    public boolean onCommand(@NotNull CommandSender sender,
                             @NotNull Command command,
                             @NotNull String label,
                             @NotNull String @NotNull [] args) {

        // Permission check: players must be moderator; console allowed
        if (sender instanceof Player player && !isAdmin(player)) {
            sender.sendMessage(chatPrefix.append(
                    Component.text("You don't have permission for this.", NamedTextColor.RED)));
            return true;
        }

        // Check: /kick <Name> [Reason]
        if (args.length < 1) {
            sender.sendMessage(chatPrefix.append(
                    Component.text("Usage: /kick <Name> [Reason]", NamedTextColor.RED)));
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            sender.sendMessage(chatPrefix.append(
                    Component.text("This player is not online.", NamedTextColor.RED)));
            return true;
        }

        // Prevent self-kick
        if (sender instanceof Player && ((Player) sender).getUniqueId().equals(target.getUniqueId())) {
            sender.sendMessage(chatPrefix.append(
                    Component.text("You cannot kick yourself.", NamedTextColor.RED)));
            return true;
        }

        // Build reason
        String reason = null;
        if (args.length >= 2) {
            reason = String.join(" ", Arrays.copyOfRange(args, 1, args.length)).trim();
            if (reason.isBlank()) reason = null;
        }

        // Kick message for player
        Component kickMessage = Component.text("You have been kicked from the server!", NamedTextColor.RED);
        if (reason != null) {
            kickMessage = kickMessage.append(Component.text("\nReason: " + reason, NamedTextColor.GRAY));
        }

        // Executor name (console friendly)
        String executor = sender instanceof Player ? sender.getName() : "the console";

        // Discord notification
        String discordMsg = target.getName() + " was kicked by " + executor
                + (reason != null ? "\nReason: " + reason : "");
        DiscordUtils.sendColoredMessage(discordMsg, 16753920);

        // Feedback to executor
        Component executorFeedback = chatPrefix.append(
                Component.text(target.getName() + " was kicked by " + executor
                        + (reason != null ? " Reason: " + reason : ""), NamedTextColor.YELLOW));
        sender.sendMessage(executorFeedback);

        // Execute kick
        target.kick(chatPrefix.append(kickMessage));

        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender,
                                                @NotNull Command command,
                                                @NotNull String alias,
                                                @NotNull String @NotNull [] args) {
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
