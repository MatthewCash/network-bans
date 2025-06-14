package com.matthewcash.network.commands;

import java.io.IOException;
import java.net.URISyntaxException;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import com.matthewcash.network.Ban;
import com.matthewcash.network.BanManager;
import com.matthewcash.network.PlayerData;
import com.matthewcash.network.IpBanManager;
import com.matthewcash.network.NetworkBans;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;

public class CheckCommand implements SimpleCommand {
    @Override
    public boolean hasPermission(final SimpleCommand.Invocation invocation) {
        return invocation.source().hasPermission("mcash.networkbans.check");
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
                        "<dark_red><bold>ERROR</bold></dark_red> <red>You must specify a player or IP address!</red>"
                    )
            );
            return;
        }

        NetworkBans.proxy.getScheduler().buildTask(NetworkBans.plugin, () -> {
            String ipAddress = null;
            PlayerData playerData = null;

            if (IpBanManager.isValidIpAddress(args[0])) {
                ipAddress = args[0];
            } else {
                try {
                    playerData = PlayerData.getPlayer(args[0]);

                    if (playerData != null) {
                        Optional<Player> proxiedPlayer = NetworkBans.proxy
                            .getPlayer(args[0]);
                        if (proxiedPlayer.isPresent()) {
                            ipAddress = IpBanManager
                                .getIpFromPlayer(proxiedPlayer.get());
                        }
                    }
                } catch (
                    InterruptedException | IOException | ParseException e
                ) {
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
            }

            if (playerData == null && ipAddress == null) {
                source.sendMessage(
                    MiniMessage.miniMessage().deserialize(
                        "<dark_red><bold>ERROR</bold></dark_red> <red>You must specify a valid player or IP address!</red>"
                    )
                );
                return;
            }

            // Check Ban
            Ban ban = null;
            if (playerData != null) {
                try {
                    ban = BanManager.getBan(playerData);
                } catch (SQLException e) {
                    NetworkBans.logger.error(
                        "Failed to check ban for "
                            + playerData.username()
                    );
                    source.sendMessage(
                        MiniMessage.miniMessage().deserialize(
                            "<dark_red><bold>ERROR</bold></dark_red> <red>Failed to check ban for <username>!</red>",
                            Placeholder
                                .unparsed("username", playerData.username())
                        )
                    );
                    e.printStackTrace();
                    return;
                }
            }

            // Check IP-Ban
            boolean isIpBanned = false;
            if (ipAddress != null) {
                try {
                    isIpBanned = IpBanManager.checkIp(ipAddress);
                } catch (
                    IOException | InterruptedException | URISyntaxException e
                ) {
                    source.sendMessage(
                        MiniMessage.miniMessage().deserialize(
                            "<dark_red><bold>ERROR</bold></dark_red> <red>Failed to check IP-ban for <ip>!</red>",
                            Placeholder.unparsed("ip", ipAddress)
                        )
                    );
                    e.printStackTrace();
                    return;
                }
            }

            // Not Banned
            if (ban == null && isIpBanned == false) {
                String name = playerData != null ? playerData.username()
                    : ipAddress;

                source.sendMessage(
                    MiniMessage.miniMessage()
                        .deserialize(
                            "<bold><green>✓</green> <gold><name></gold></bold> <gray>is not banned</gray>",
                            Placeholder.unparsed("name", name)
                        )
                );
                return;
            }

            if (isIpBanned) {
                source.sendMessage(
                    MiniMessage.miniMessage().deserialize(
                        "<bold><red>✖</red> <gold><ip></gold></bold> <gray>has been <red><bold>IP-BANNED</bold></red>!</gray>",
                        Placeholder.unparsed("ip", ipAddress)
                    )
                );
            }

            if (ban != null) {
                Component banComponent = MiniMessage.miniMessage().deserialize(
                    "<bold><red>✖</red> <gold><username></gold></bold> <gray>has been <red><bold>BANNED</bold></red> with reason <gold><bold><reason></bold></gold></gray>",
                    Placeholder.unparsed("username", playerData.username()),
                    Placeholder.unparsed("reason", ban.reason())
                );

                if (ban.unbanTime() == null) {
                    source.sendMessage(
                        banComponent.append(
                            Component.text("!").color(NamedTextColor.GRAY)
                        )
                    );
                    return;
                }

                source.sendMessage(
                    banComponent.append(
                        MiniMessage.miniMessage().deserialize(
                            "<gray> until <gold><bold><time></bold></gold>!</gray>",
                            Placeholder.unparsed(
                                "time",
                                DateFormat.getDateTimeInstance()
                                    .format(ban.unbanTime())
                            )
                        )
                    )
                );

            }
        })
            .schedule();
    }
}
