package de.jakomi1.betterBan;

import de.jakomi1.betterBan.commands.*;
import de.jakomi1.betterBan.database.Database;
import de.jakomi1.betterBan.listener.JoinListener;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import javax.xml.crypto.Data;
import java.io.File;
import java.util.Objects;

public final class BetterBan extends JavaPlugin {
    public static final Component chatPrefix = Component.text("[", NamedTextColor.GRAY)
            .append(Component.text("BB", NamedTextColor.DARK_RED))
            .append(Component.text("] ", NamedTextColor.GRAY));
    public static Plugin plugin;
    public static File dataFolder;
    @Override
    public void onEnable() {
        plugin = this;
        dataFolder = getDataFolder();
        if (!dataFolder.exists()) {
            if (dataFolder.mkdirs()) {
                getLogger().info("Created plugin folder: " + dataFolder.getPath());
            } else {
                getLogger().warning("Couldn't create plugin folder: " + dataFolder.getPath());
            }
        }
        Database.init();
        registerCommands();
        registerListeners();
    }

    private void registerCommands() {
        registerCommand("ban", new BanCommand(), new BanCommand());
        registerCommand("banlist", new BanListCommand(), new EmptyTabCompleter());
        registerCommand("unban", new UnbanCommand(), new UnbanCommand());
        registerCommand("tempban", new TempBanCommand(), new TempBanCommand());
        registerCommand("kick", new KickCommand(), new KickCommand());
    }

    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new JoinListener(), this);
    }

    private void registerCommand(String command, CommandExecutor executor, TabCompleter tabCompleter) {
        Objects.requireNonNull(getServer().getPluginCommand(command)).setExecutor(executor);
        Objects.requireNonNull(getServer().getPluginCommand(command)).setTabCompleter(tabCompleter);
    }


    @Override
    public void onDisable() {

    }
    public static boolean isAdmin(Player player) {
        return player.isOp();
    }


}
