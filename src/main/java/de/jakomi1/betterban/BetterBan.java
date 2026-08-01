package de.jakomi1.betterban;

import de.jakomi1.betterban.command.*;
import de.jakomi1.betterban.database.Database;
import de.jakomi1.betterban.listener.ChatListener;
import de.jakomi1.betterban.listener.JoinListener;
import de.jakomi1.betterban.util.ConfigUtils;
import dev.faststats.bukkit.BukkitContext;
import org.bstats.bukkit.Metrics;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.Objects;

import static de.jakomi1.betterban.util.CommandUtils.registerDynamicCommand;
import static de.jakomi1.betterban.util.ConfigUtils.loadConfig;
import static de.jakomi1.betterban.util.PermissionUtils.registerPermission;

public final class BetterBan extends JavaPlugin {
    public static String pluginId = "betterban";
    public static boolean isVoiceChatConnected = false;
    public static String chatPrefix ;
    public static Plugin plugin;
    public static File dataFolder;
    private final BukkitContext context = new BukkitContext.Factory(this, "3189aeb1032d774045215754244beab4")
            .metrics(dev.faststats.Metrics.Factory::create)
            .create();

    @Override
    public void onEnable() {
        plugin = this;
        context.ready();
        dataFolder = getDataFolder();
        new Metrics(this, 32083);
        loadConfig();
        if (!dataFolder.exists()) {
            if (dataFolder.mkdirs()) {
                getLogger().info("Created plugin folder: " + dataFolder.getPath());
            } else {
                getLogger().warning("Couldn't create plugin folder: " + dataFolder.getPath());
            }
        }

        if (getServer().getPluginManager().isPluginEnabled("voicechat")) {
            try {
                Class<?> serviceClass =
                        Class.forName("de.maxhenkel.voicechat.api.BukkitVoicechatService");

                Object service = getServer()
                        .getServicesManager()
                        .load((Class) serviceClass);

                if (service != null) {
                    Class<?> integrationClass =
                            Class.forName("de.jakomi1.betterban.integration.voicechat.VoiceChatIntegration");

                    Object integration =
                            integrationClass.getDeclaredConstructor().newInstance();

                    serviceClass
                            .getMethod("registerPlugin",
                                    Class.forName("de.maxhenkel.voicechat.api.VoicechatPlugin"))
                            .invoke(service, integration);

                    getLogger().info("SimpleVoiceChat is installed and loaded");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        Database.init();
        registerCommands();
        chatPrefix = ConfigUtils.getPrefixStyled();
        registerListeners();
    }

    private void registerCommands() {
        registerDynamicCommand("ban", new BanCommand(), new BanCommand(), registerPermission(pluginId+".ban", PermissionDefault.OP));
        registerDynamicCommand("banlist", new BanListCommand(), new EmptyTabCompleter(), registerPermission(pluginId+".banlist", PermissionDefault.OP));
        registerDynamicCommand("unban", new UnbanCommand(), new UnbanCommand(), registerPermission(pluginId+".unban", PermissionDefault.OP));
        registerDynamicCommand("tempban", new TempBanCommand(), new TempBanCommand(), registerPermission(pluginId+".tempban", PermissionDefault.OP));
        registerDynamicCommand("kick", new KickCommand(), new KickCommand(), registerPermission(pluginId+".kick", PermissionDefault.OP));
        registerDynamicCommand("chatban", new ChatBanCommand(), new ChatBanCommand(), registerPermission(pluginId+".chat.ban", PermissionDefault.OP));
        registerDynamicCommand("chatbanlist", new ChatBanListCommand(), new EmptyTabCompleter(), registerPermission(pluginId+".chat.banlist", PermissionDefault.OP));
        registerDynamicCommand("chatunban", new ChatUnbanCommand(), new ChatUnbanCommand(), registerPermission(pluginId+".chat.unban", PermissionDefault.OP));
    }

    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new JoinListener(), this);
        getServer().getPluginManager().registerEvents(new ChatListener(), this);
    }

    private void registerCommand(String command, CommandExecutor executor, TabCompleter tabCompleter) {
        Objects.requireNonNull(getServer().getPluginCommand(command)).setExecutor(executor);
        Objects.requireNonNull(getServer().getPluginCommand(command)).setTabCompleter(tabCompleter);
    }

    private YamlConfiguration lang;


    @Override
    public void onDisable() {
        context.shutdown();
    }
    public static boolean isAdmin(Player player) {
        return player.isOp();
    }


}
