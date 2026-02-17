package de.jakomi1.betterBan.listener;

import de.jakomi1.betterBan.utils.BanUtils;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.UUID;

public class JoinListener implements Listener {
    /*@EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        //.joinMessage(chatPrefix.append(Component.text(player.getName() + " hat den Server betreten", NamedTextColor.YELLOW)));

    }*/
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onAsyncPreLogin(AsyncPlayerPreLoginEvent event) {
        String name = event.getName();
        UUID uuid = event.getUniqueId();
        String ip = event.getAddress().getHostAddress();


        if (!BanUtils.hasJoinedBefore(uuid)) {
            BanUtils.markJoined(uuid, name);
        }

        // Bannprüfung
        if (BanUtils.isBanned(uuid)) {
            if (BanUtils.isPermanent(uuid)) {
                event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_BANNED,
                        BanUtils.getBanMessage(uuid));
            } else {
                event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_BANNED,
                        BanUtils.getBanMessage(uuid));
            }
            return;
        }

        // Whitelist
        /*if (!WhitelistCommand.isWhitelisted(name)) {
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_WHITELIST,
                    chatPrefix.append(Component.text("Lasse dich erst auf unserem Discord whitelisten!\n", NamedTextColor.RED))
                            .append(Component.text("Link: https://discord.gg/" + discordInviteToken, NamedTextColor.GRAY)));
            return;
        }*/

        // Alles bestanden → Join erlauben
        event.allow();
    }
}
