package com.matthewcash.network.commands;

import java.io.IOException;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.Collections;
import java.util.List;

import com.matthewcash.network.BanManager;
import com.matthewcash.network.PlayerData;
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
            PlayerData playerData;
            try {
                playerData = PlayerData.getPlayer(args[0]);
            } catch (InterruptedException | IOException | ParseException e) {
                source.sendMessage(
                    MiniMessage.miniMessage()
                        .deserialize(
                            "<dark_red><bold>ERROR</bold></dark_red> <red>Failed to lookup UUID for <player>!</red>",
                            Placeholder.unparsed("player", args[0])
                        )
                );
                e.printStackTrace();
                return;
            }

            if (playerData == null) {
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
                if (BanManager.getBan(playerData) == null) {
                    source.sendMessage(
                        MiniMessage.miniMessage()
                            .deserialize(
                                "<dark_red><bold>ERROR</bold></dark_red> <red>Player <player> is not banned!</red>",
                                Placeholder
                                    .unparsed("player", playerData.username())
                            )
                    );
                    return;
                }
            } catch (SQLException e) {
                NetworkBans.logger.error(
                    "Failed to check ban for "
                        + playerData.username()
                );
                e.printStackTrace();

                source.sendMessage(
                    MiniMessage.miniMessage().deserialize(
                        "<dark_red><bold>ERROR</bold></dark_red> <red>Failed to check ban for <username>!</red>",
                        Placeholder.unparsed("username", playerData.username())
                    )
                );
                return;
            }

            // Unban Player
            try {
                BanManager.unban(playerData);
            } catch (SQLException e) {
                NetworkBans.logger
                    .error(
                        "Failed to unban " + playerData.username()
                    );
                e.printStackTrace();

                source.sendMessage(
                    MiniMessage.miniMessage().deserialize(
                        "<dark_red><bold>ERROR</bold></dark_red> <red>Failed to unban <username>!</red>",
                        Placeholder.unparsed("username", playerData.username())
                    )
                );
                return;
            }

            source.sendMessage(
                MiniMessage.miniMessage()
                    .deserialize(
                        "<gray>You have unbanned <gold><bold><username></bold></gold>!</gray>",
                        Placeholder.unparsed("username", playerData.username())
                    )
            );

        }).schedule();

    }
}
