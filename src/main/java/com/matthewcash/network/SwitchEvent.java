package com.matthewcash.network;

import java.sql.SQLException;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.ServerConnectEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.PluginLogger;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.event.EventPriority;

public class SwitchEvent implements Listener {
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onServerConnect(ServerConnectEvent event) {
        ProxiedPlayer player = event.getPlayer();

        // Ignore if Player is connecting to hub
        if (event.getTarget().getName().equals("hub")) {
            return;
        }

        BanPlayer banPlayer = BanPlayer.getPlayer(player.getName());

        // Check if player is banned
        Ban ban = null;
        try {
            ban = BanManager.getBan(banPlayer);
        } catch (SQLException e) {
            PluginLogger.getLogger("NetworkBan").severe("Error occured while checking ban for " + banPlayer.username);
            e.printStackTrace();
        }
        if (ban == null) {
            return;
        }

        // Player is banned, stop connection
        event.setCancelled(true);

        // Send ban message
        player.sendMessage(new ComponentBuilder("").create());
        player.sendMessage(new ComponentBuilder("You have been ").color(ChatColor.RED).bold(true).append("BANNED")
                .color(ChatColor.DARK_RED).bold(true).append(" and may no longer connect to this server!")
                .color(ChatColor.RED).bold(true).create());
        player.sendMessage(new ComponentBuilder(ban.reason).create());
        player.sendMessage(new ComponentBuilder("").create());
    }

}