package de.jakomi1.betterban.integration.voicechat;

import de.jakomi1.betterban.BetterBan;
import de.jakomi1.betterban.command.EmptyTabCompleter;
import de.jakomi1.betterban.integration.voicechat.command.VoiceBanCommand;
import de.jakomi1.betterban.integration.voicechat.command.VoiceBanListCommand;
import de.jakomi1.betterban.integration.voicechat.command.VoiceUnbanCommand;
import de.jakomi1.betterban.integration.voicechat.listener.PacketListener;
import de.jakomi1.betterban.integration.voicechat.utils.VoiceBanUtils;
import de.maxhenkel.voicechat.api.VoicechatApi;
import de.maxhenkel.voicechat.api.VoicechatPlugin;
import de.maxhenkel.voicechat.api.VoicechatServerApi;
import de.maxhenkel.voicechat.api.events.*;
import org.bukkit.permissions.PermissionDefault;

import java.util.List;

import static de.jakomi1.betterban.BetterBan.pluginId;
import static de.jakomi1.betterban.util.CommandUtils.registerDynamicCommand;
import static de.jakomi1.betterban.util.PermissionUtils.registerPermission;

public class VoiceChatIntegration implements VoicechatPlugin {

    private static volatile VoicechatServerApi serverApi;

    public static VoicechatServerApi getApi() {
        return serverApi;
    }

    @Override
    public String getPluginId() {
        return pluginId;
    }

    @Override
    public void registerEvents(EventRegistration registration) {
        registration.registerEvent(MicrophonePacketEvent.class, PacketListener::onMicrophonePacketEvent);
        registration.registerEvent(StaticSoundPacketEvent.class, PacketListener::onSoundPacketEvent);
        registration.registerEvent(LocationalSoundPacketEvent.class, PacketListener::onSoundPacketEvent);
        registration.registerEvent(EntitySoundPacketEvent.class, PacketListener::onSoundPacketEvent);
        registration.registerEvent(JoinGroupEvent.class, PacketListener::onGroupEvent);
    }

    @Override
    public void initialize(VoicechatApi api) {
        if (api instanceof VoicechatServerApi s) {
            serverApi = s;
        } else {
            return;
        }

        BetterBan.isVoiceChatConnected = true;
        BetterBan.plugin.getLogger().info(
                "BetterBan is connected with SimpleVoiceChat"
        );

        registerDynamicCommand("voiceban", new VoiceBanCommand(), new VoiceBanCommand(), List.of("mute"), registerPermission(pluginId + ".voice.ban", PermissionDefault.OP));
        registerDynamicCommand("voicebanlist", new VoiceBanListCommand(), new EmptyTabCompleter(), registerPermission(pluginId + ".voice.banlist", PermissionDefault.OP));
        registerDynamicCommand("voiceunban", new VoiceUnbanCommand(), new VoiceUnbanCommand(), List.of("unmute"), registerPermission(pluginId + ".voice.unban", PermissionDefault.OP));
        VoiceBanUtils.init();
    }
}