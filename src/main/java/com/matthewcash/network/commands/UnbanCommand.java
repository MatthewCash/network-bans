package com.matthewcash.network.commands;

import java.sql.SQLException;
import java.util.Collections;
import java.util.List;

import com.matthewcash.network.BanManager;
import com.matthewcash.network.BanPlayer;
import com.matthewcash.network.NetworkBans;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;

import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;

public class UnbanCommand implements SimpleCommand {
    @Override
    public boolean hasPermission(final SimpleCommand.Invocation invocation) {
        return invocation.source().hasPermission("mcash.networkbans.unban");
    }

    @Override
    public List<String> suggest(final SimpleCommand.Invocation invocation) {
        if (invocation.arguments().length > 1) {
            return Collections.emptyList();
        }

        return NetworkBans.proxy.getAllPlayers().stream()
            .map(player -> player.getUsername()).toList();
    }

    @Override
    public void execute(final SimpleCommand.Invocation invocation) {
        CommandSource source = invocation.source();
        String[] args = invocation.arguments();

        if (args.length < 1) {
            source.sendMessage(
                MiniMessage.miniMessage()
                    .deserialize(
                        "<dark_red><bold>ERROR</bold></dark_red> <red>You must specify a player!</red>"
                    )
            );
            return;
        }

        NetworkBans.proxy.getScheduler().buildTask(NetworkBans.plugin, () -> {
            // Lookup UUID
            BanPlayer player = BanPlayer.getPlayer(args[0]);

            if (player == null) {
                source.sendMessage(
                    MiniMessage.miniMessage()
                        .deserialize(
                            "<dark_red><bold>ERROR</bold></dark_red> <red>Player <player> could not be found!</red>",
                            Placeholder.unparsed("player", args[0])
                        )
                );
                return;
            }

            // Check if Player is banned
            try {
                if (BanManager.getBan(player) == null) {
                    source.sendMessage(
                        MiniMessage.miniMessage()
                            .deserialize(
                                "<dark_red><bold>ERROR</bold></dark_red> <red>Player <player> is not banned!</red>",
                                Placeholder.unparsed("player", player.username)
                            )
                    );
                    return;
                }
            } catch (SQLException e) {
                NetworkBans.logger.error(
                    "Error occurred while checking ban for " + player.username
                );
                e.printStackTrace();

                source.sendMessage(
                    MiniMessage.miniMessage().deserialize(
                        "<dark_red><bold>ERROR</bold></dark_red> <red>An error occurred while checking ban for <username>!</red>",
                        Placeholder.unparsed("username", player.username)
                    )
                );
                return;
            }

            // Unban Player
            try {
                BanManager.unban(player);
            } catch (SQLException e) {
                NetworkBans.logger
                    .error("Error occurred while unbanning " + player.username);
                e.printStackTrace();

                source.sendMessage(
                    MiniMessage.miniMessage().deserialize(
                        "<dark_red><bold>ERROR</bold></dark_red> <red>An error occurred while unbanning <username>!</red>",
                        Placeholder.unparsed("username", player.username)
                    )
                );
                return;
            }

            source.sendMessage(
                MiniMessage.miniMessage()
                    .deserialize(
                        "<gray>You have unbanned <gold><bold><username></bold></gold>!</gray>",
                        Placeholder.unparsed("username", player.username)
                    )
            );

        }).schedule();

    }
}
