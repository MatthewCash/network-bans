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
    public void onServerPreConnect(ServerPreConnectEvent event) {
        Player player = event.getPlayer();

        RegisteredServer server = event.getResult().getServer().get();
        RegisteredServer hubServer = NetworkBans.proxy.getServer("hub").get();

        // Ignore if Player is connecting to hub
        if (server == null || server == hubServer) {
            return;
        }

        BanPlayer banPlayer = BanPlayer.getPlayer(player.getUsername());

        // Check if player is banned
        Ban ban = null;
        try {
            ban = BanManager.getBan(banPlayer);
        } catch (SQLException e) {
            NetworkBans.logger.error(
                "Error occurred while checking ban for " + banPlayer.username
            );
            e.printStackTrace();
        }

        // Player is not banned, continue
        if (ban == null) {
            return;
        }

        // Player is banned, stop connection or redirect to hub
        if (player.getCurrentServer().isPresent()) {
            event.setResult(ServerPreConnectEvent.ServerResult.denied());
        } else {
            event.setResult(
                ServerPreConnectEvent.ServerResult.allowed(hubServer)
            );

        }

        // Send ban message
        player.sendMessage(
            MiniMessage.miniMessage()
                .deserialize(
                    "<newline><bold><red>You have been <dark_red>BANNED</dark_red> and may no longer connect to this server!</red></bold><newline><reason><newline>",
                    Placeholder.unparsed("reason", ban.reason)
                )
        );
    }

}
