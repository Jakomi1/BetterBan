package de.jakomi1.betterban.integration.voicechat.command;

import de.jakomi1.betterban.integration.voicechat.utils.VoiceBanUtils;
import de.jakomi1.betterban.util.DiscordUtils;
import de.jakomi1.betterban.util.TextUtils;
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

import static de.jakomi1.betterban.BetterBan.chatPrefix;
import static de.jakomi1.betterban.BetterBan.isAdmin;

public class VoiceUnbanCommand implements CommandExecutor, TabCompleter {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player) || isAdmin(player)) {
            if (args.length < 1) {
                sender.sendMessage(chatPrefix + ChatColor.RED + TextUtils.lang("messages.error.player_required"));
                return true;
            }

            OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
            UUID uuid = target.getUniqueId();

            if (!VoiceBanUtils.isVoiceBanned(uuid)) {
                sender.sendMessage(chatPrefix + ChatColor.RED +
                        (target.getName() != null ? target.getName() : uuid.toString()) + " " + TextUtils.lang("messages.error.not_voice_banned"));
                return true;
            }

            VoiceBanUtils.voiceUnban(uuid);

            String name = target.getName() != null ? target.getName() : uuid.toString();
            sender.sendMessage(chatPrefix + ChatColor.GRAY + name + " " + TextUtils.lang("messages.success.voiceunbanned"));

            String executor = sender instanceof Player ? sender.getName() : "the console";
            DiscordUtils.sendVoiceUnbanWebhook(name, executor);
            Player onlineTarget = Bukkit.getPlayer(uuid);
            if (onlineTarget != null && onlineTarget.isOnline()) {
                onlineTarget.sendMessage(chatPrefix + ChatColor.GRAY + TextUtils.lang("messages.success.voiceunbanned_you", "executor", executor));
            }

        } else {
            sender.sendMessage(chatPrefix + ChatColor.RED + TextUtils.lang("messages.error.no_permission"));
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (sender instanceof Player player && !isAdmin(player)) return List.of();

        if (args.length == 1) {
            return VoiceBanUtils.getAllVoiceBans().keySet().stream()
                    .map(Bukkit::getOfflinePlayer)
                    .map(OfflinePlayer::getName)
                    .filter(Objects::nonNull)
                    .filter(name -> name.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }

        return List.of();
    }
}