package de.jakomi1.betterBan.commands;

import de.jakomi1.betterBan.utils.BanUtils;
import de.jakomi1.betterBan.utils.ChatBanUtils;
import de.jakomi1.betterBan.utils.DiscordUtils;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.ChatColor;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static de.jakomi1.betterBan.BetterBan.chatPrefix;
import static de.jakomi1.betterBan.BetterBan.isAdmin;

public class ChatBanCommand implements CommandExecutor, TabCompleter {

    @Override
    public boolean onCommand(CommandSender sender,
                             Command command,
                             String label,
                             String[] args) {
        // Permission check
        if (sender instanceof Player player && !isAdmin(player)) {
            sender.sendMessage(chatPrefix + ChatColor.RED + "You don't have permission for this.");
            return true;
        }

        // /chatban <Name> <Duration|permanent> [Reason...]
        if (args.length < 2) {
            sender.sendMessage(chatPrefix + ChatColor.RED +
                    "Usage: /chatban <Name> <Duration> [Reason...]");
            return true;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
        UUID uuid = target.getUniqueId();

        // Check if player has ever joined
        if (!BanUtils.hasJoinedBefore(uuid)) {
            sender.sendMessage(chatPrefix + ChatColor.RED + "This player has never joined the server.");
            return true;
        }

        // Check if player is already chat-banned
        if (ChatBanUtils.isChatBanned(uuid)) {
            sender.sendMessage(chatPrefix + ChatColor.RED +
                    (target.getName() != null ? target.getName() : uuid.toString()) + " is already chat-banned!");
            return true;
        }

        String durationRaw = args[1].toLowerCase();
        boolean permanent = durationRaw.equalsIgnoreCase("permanent");
        long delta = 0L;

        if (!permanent) {
            try {
                delta = parseDuration(durationRaw);
            } catch (IllegalArgumentException e) {
                sender.sendMessage(chatPrefix + ChatColor.RED +
                        "Invalid time format. Example: 10m, 2h, 1d, or 'permanent'.");
                return true;
            }
        }

        // Optional reason
        String reason = args.length >= 3 ? String.join(" ", Arrays.copyOfRange(args, 2, args.length)).trim() : null;

        // Save chat-ban in DB/cache
        if (permanent) {
            ChatBanUtils.chatPermanentBan(uuid, reason);
        } else {
            ChatBanUtils.chatTempBan(uuid, delta, reason);
        }

        String name = Objects.toString(target.getName(), uuid.toString());
        String executor = sender instanceof Player ? sender.getName() : "the console";
        String remaining = permanent ? "permanent" : BanUtils.formatDuration(delta);

        // Feedback to executor
        sender.sendMessage(chatPrefix + ChatColor.YELLOW +
                name + " has been chat-banned (" + remaining + ").");
        if (reason != null && !reason.isBlank()) {
            sender.sendMessage(chatPrefix + ChatColor.GRAY + "Reason: " + reason);
        }

        // Discord notification (use an orange-ish color for mutes)
        DiscordUtils.sendColoredMessage(
                name + " was chat-banned by " + executor + " (" + remaining + ")" +
                        (reason != null ? "\n*Reason: " + reason : "*"),
                0xFFA500
        );

        // Notify the target player if they are online
        Player onlineTarget = Bukkit.getPlayer(uuid); // returns null if offline
        if (onlineTarget != null && onlineTarget.isOnline()) {
            // Inform the player who got muted
            onlineTarget.sendMessage(chatPrefix + ChatColor.YELLOW +
                    "You have been chat-banned by " + executor + " (" + remaining + ").");
            if (reason != null && !reason.isBlank()) {
                onlineTarget.sendMessage(chatPrefix + ChatColor.GRAY + "Reason: " + reason);
            }
            // Optional: also send the standardized chat-ban message used when they try to chat
            // (keeps consistency with ChatListener). Uncomment if you want that as well:
            // onlineTarget.sendMessage(ChatBanUtils.getChatBanMessage(uuid));
        }

        // Do NOT kick — chat-ban only mutes chat. (If you want to kick as well, we can add it.)

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
    public List<String> onTabComplete(CommandSender sender,
                                      Command command,
                                      String alias,
                                      String[] args) {
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
            return Stream.of("10m", "30m", "1h", "2h", "1d", "permanent")
                    .filter(opt -> opt.startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
        }

        return List.of();
    }
}
