package de.jakomi1.betterban.integration.voicechat.listener;

import de.jakomi1.betterban.integration.voicechat.utils.VoiceBanUtils;
import de.jakomi1.betterban.util.TextUtils;
import de.maxhenkel.voicechat.api.ServerPlayer;
import de.maxhenkel.voicechat.api.VoicechatConnection;
import de.maxhenkel.voicechat.api.events.*;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.UUID;

public class PacketListener {
    public static void onGroupEvent(JoinGroupEvent event) {
        handlePacketEvent(event, event.getConnection());
    }


    public static void onMicrophonePacketEvent(MicrophonePacketEvent event) {
        handlePacketEvent(event, event.getSenderConnection());
    }
    public static void onSoundPacketEvent(StaticSoundPacketEvent event) {
        handlePacketEvent(event, event.getReceiverConnection());
    }
    public static void onSoundPacketEvent(EntitySoundPacketEvent event) {
        handlePacketEvent(event, event.getReceiverConnection());
    }
    public static void onSoundPacketEvent(LocationalSoundPacketEvent event) {
        handlePacketEvent(event, event.getReceiverConnection());
    }

    public static void handlePacketEvent(Event event, VoicechatConnection senderConnection) {
        if (event == null || senderConnection == null) {
            return;
        }
        ServerPlayer serverPlayer = senderConnection.getPlayer();
        UUID uuid = serverPlayer.getUuid();
        if(VoiceBanUtils.isVoiceBanned(uuid)) {
            event.cancel();
            if(serverPlayer.getPlayer() instanceof Player player) {
                TextUtils.sendActionBar(player, ChatColor.RED + VoiceBanUtils.getVoiceBanMessage(uuid));
            }
        }
    }
}