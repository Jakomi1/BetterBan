package de.jakomi1.betterBan.listener;

import de.jakomi1.betterBan.utils.ChatBanUtils;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import java.util.UUID;

public class ChatListener implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        if (ChatBanUtils.isChatBanned(uuid)) {
            event.setCancelled(true);
            player.sendMessage(ChatBanUtils.getChatBanMessage(uuid));
        }
    }

    private static final String[] MESSAGE_COMMANDS = {
            "msg", "tell", "w", "whisper","teammsg", "minecraft:teammsg",
            "minecraft:msg", "minecraft:tell", "minecraft:w", "minecraft:whisper"
    };

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        if (!ChatBanUtils.isChatBanned(uuid)) return;

        String message = event.getMessage().toLowerCase();

        for (String cmd : MESSAGE_COMMANDS) {
            if (message.startsWith("/" + cmd + " ")) {
                event.setCancelled(true);
                player.sendMessage(ChatBanUtils.getChatBanMessage(uuid));
                return;
            }
        }
    }
}
