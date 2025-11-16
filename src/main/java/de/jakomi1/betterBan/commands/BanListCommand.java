package de.jakomi1.betterBan.commands;

import de.jakomi1.betterBan.utils.BanUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.UUID;

import static de.jakomi1.betterBan.BetterBan.chatPrefix;

public class BanListCommand implements CommandExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender sender,
                             @NotNull Command command,
                             @NotNull String label,
                             @NotNull String[] args) {

        // Automatically remove old/expired bans
        BanUtils.clearExpiredBans();

        Map<UUID, Map<String, Object>> bans = BanUtils.getAllBans();

        if (bans.isEmpty()) {
            sender.sendMessage(chatPrefix.append(Component.text("No players are banned.", NamedTextColor.GRAY)));
            return true;
        }

        sender.sendMessage(chatPrefix.append(Component.text("Banned players:", NamedTextColor.YELLOW)));

        for (Map.Entry<UUID, Map<String, Object>> entry : bans.entrySet()) {
            UUID uuid = entry.getKey();
            Map<String, Object> data = entry.getValue();
            OfflinePlayer off = Bukkit.getOfflinePlayer(uuid);
            String name = off.getName() != null ? off.getName() : uuid.toString();

            long endTimestamp = (long) data.get("end_timestamp");
            String reason = (String) data.get("reason");

            String suffix;
            if (endTimestamp == -1) {
                suffix = "Permanent";
            } else {
                long rem = endTimestamp - System.currentTimeMillis();
                if (rem < 0) rem = 0;
                suffix = BanUtils.formatDuration(rem);
            }

            sender.sendMessage(Component.text(name + " >> " + suffix, NamedTextColor.GRAY));

            if (reason != null && !reason.isBlank()) {
                sender.sendMessage(Component.text("-> Reason: " + reason, NamedTextColor.DARK_GRAY));
            }
        }

        return true;
    }
}
