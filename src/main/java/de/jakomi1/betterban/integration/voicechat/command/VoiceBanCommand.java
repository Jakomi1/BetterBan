package de.jakomi1.betterban.integration.voicechat.command;

import de.jakomi1.betterban.integration.voicechat.VoiceChatIntegration;
import de.jakomi1.betterban.integration.voicechat.utils.VoiceBanUtils;
import de.jakomi1.betterban.util.BanUtils;
import de.jakomi1.betterban.util.DiscordUtils;
import de.jakomi1.betterban.util.TextUtils;
import de.maxhenkel.voicechat.api.Group;
import de.maxhenkel.voicechat.api.VoicechatConnection;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static de.jakomi1.betterban.BetterBan.*;
import static de.jakomi1.betterban.util.TextUtils.parseDuration;

public class VoiceBanCommand implements CommandExecutor, TabCompleter {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player player && !isAdmin(player)) {
            sender.sendMessage(chatPrefix + ChatColor.RED + TextUtils.lang("messages.error.no_permission"));
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(chatPrefix + ChatColor.RED + TextUtils.lang("messages.error.usage_voiceban"));
            return true;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
        UUID uuid = target.getUniqueId();

        if (!BanUtils.hasJoinedBefore(uuid)) {
            sender.sendMessage(chatPrefix + ChatColor.RED + TextUtils.lang("messages.error.player_never_joined"));
            return true;
        }

        if (VoiceBanUtils.isVoiceBanned(uuid)) {
            sender.sendMessage(chatPrefix + ChatColor.RED +
                    (target.getName() != null ? target.getName() : uuid.toString()) + " " + TextUtils.lang("messages.error.already_voice_banned"));
            return true;
        }

        String durationRaw = args[1].toLowerCase();
        boolean permanent = durationRaw.equalsIgnoreCase("permanent");
        long delta = 0L;

        if (!permanent) {
            try {
                delta = parseDuration(durationRaw);
            } catch (IllegalArgumentException e) {
                sender.sendMessage(chatPrefix + ChatColor.RED + TextUtils.lang("messages.error.invalid_time"));
                return true;
            }
        }

        String reason = args.length >= 3 ? String.join(" ", Arrays.copyOfRange(args, 2, args.length)).trim() : null;

        var conn = VoiceChatIntegration.getApi().getConnectionOf(uuid);
        if (conn != null && conn.getGroup() != null) {
            Group group = conn.getGroup();
            conn.setGroup(null);

            long remaining = Bukkit.getOnlinePlayers().stream()
                    .map(pm -> VoiceChatIntegration.getApi().getConnectionOf(pm.getUniqueId()))
                    .filter(Objects::nonNull)
                    .map(VoicechatConnection::getGroup)
                    .filter(group::equals)
                    .count();

            if (remaining == 0 && !group.isPersistent()) {
                VoiceChatIntegration.getApi().removeGroup(group.getId());
            }
        }

        if (permanent) {
            VoiceBanUtils.voicePermanentBan(uuid, reason);
        } else {
            VoiceBanUtils.voiceTempBan(uuid, delta, reason);
        }

        String name = Objects.toString(target.getName(), uuid.toString());
        String executor = sender instanceof Player ? sender.getName() : "the console";
        String remaining = permanent ? TextUtils.lang("messages.list.permanent") : BanUtils.formatDuration(delta);

        sender.sendMessage(chatPrefix + ChatColor.YELLOW + TextUtils.lang("messages.success.voicebanned", "player", name, "duration", remaining));
        if (reason != null && !reason.isBlank()) {
            sender.sendMessage(chatPrefix + ChatColor.GRAY + TextUtils.lang("messages.success.reason", "reason", reason));
        }
        if(permanent) {
            DiscordUtils.sendVoiceBanPermanentWebhook(target.getName(), executor, reason);
        } else {
            DiscordUtils.sendVoiceBanTempWebhook(target.getName(), executor, remaining, reason);
        }
        Player onlineTarget = Bukkit.getPlayer(uuid);
        if (onlineTarget != null && onlineTarget.isOnline()) {
            onlineTarget.sendMessage(chatPrefix + ChatColor.YELLOW +
                    TextUtils.lang("messages.success.voicebanned_you", "executor", executor, "duration", remaining));
            if (reason != null && !reason.isBlank()) {
                onlineTarget.sendMessage(chatPrefix + ChatColor.GRAY + TextUtils.lang("messages.success.reason", "reason", reason));
            }
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (sender instanceof Player player && !isAdmin(player)) return List.of();

        if (args.length == 1) {
            return Arrays.stream(Bukkit.getOfflinePlayers())
                    .filter(p -> BanUtils.hasJoinedBefore(p.getUniqueId()))
                    .map(OfflinePlayer::getName)
                    .filter(Objects::nonNull)
                    .filter(n -> n.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length == 2) {
            return Stream.of("10m", "30m", "1h", "2h", "1d", "permanent")
                    .filter(opt -> opt.startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
        }

        return List.of();
    }
}