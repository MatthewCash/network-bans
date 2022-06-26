package com.matthewcash.network;

import java.sql.SQLException;

import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.ServerPreConnectEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.server.RegisteredServer;

import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;

public class SwitchEvent {
    @Subscribe(order = PostOrder.LAST)
    public void onServerConnect(ServerPreConnectEvent event) {
        Player player = event.getPlayer();

        RegisteredServer server = event.getResult().getServer().get();

        // Ignore if Player is connecting to hub
        if (server == null || server.getServerInfo().getName().equals("hub")) {
            return;
        }

        BanPlayer banPlayer = BanPlayer.getPlayer(player.getUsername());

        // Check if player is banned
        Ban ban = null;
        try {
            ban = BanManager.getBan(banPlayer);
        } catch (SQLException e) {
            NetworkBans.logger.error("Error occurred while checking ban for " + banPlayer.username);
            e.printStackTrace();
        }

        // Player is not banned, continue
        if (ban == null) {
            return;
        }

        // Player is banned, stop connection
        event.setResult(ServerPreConnectEvent.ServerResult.denied());

        // Send ban message

        // player.sendMessage(new ComponentBuilder("").create());
        // player.sendMessage(new ComponentBuilder("You have been
        // ").color(ChatColor.RED).bold(true).append("BANNED")
        // .color(ChatColor.DARK_RED).bold(true).append(" and may no longer connect to
        // this server!")
        // .color(ChatColor.RED).bold(true).create());
        // player.sendMessage(new ComponentBuilder(ban.reason).create());
        // player.sendMessage(new ComponentBuilder("").create());

        player.sendMessage(MiniMessage.miniMessage()
            .deserialize(
                "<newline><bold><red>You have been <dark_red>BANNED</dark_red> and may no longer connect to this server!</red></bold><newline><reason><newline>",
                Placeholder.unparsed("reason", ban.reason)));
    }

}