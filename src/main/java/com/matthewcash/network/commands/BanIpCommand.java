package com.matthewcash.network.commands;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import com.matthewcash.network.IpBanManager;
import com.matthewcash.network.NetworkBans;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;

import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;

public class BanIpCommand implements SimpleCommand {
    @Override
    public boolean hasPermission(final SimpleCommand.Invocation invocation) {
        return invocation.source().hasPermission("mcash.networkbans.banip");
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

        Optional<Player> player = NetworkBans.proxy.getPlayer(args[0]);
        String ipAddress = player.isPresent()
            ? IpBanManager.getIpFromPlayer(player.get())
            : args[0];

        if (!IpBanManager.isValidIpAddress(ipAddress)) {
            source.sendMessage(
                MiniMessage.miniMessage()
                    .deserialize(
                        "<dark_red><bold>ERROR</bold></dark_red> <red>You must specify a player or IP address!</red>"
                    )
            );
            return;
        }

        NetworkBans.proxy.getScheduler().buildTask(NetworkBans.plugin, () -> {
            try {
                if (IpBanManager.checkIp(ipAddress)) {
                    source.sendMessage(
                        MiniMessage.miniMessage()
                            .deserialize(
                                "<dark_red><bold>ERROR</bold></dark_red> <red>IP Address <ip> is already banned!</red>",
                                Placeholder.unparsed("ip", ipAddress)
                            )
                    );
                    return;
                }

                IpBanManager.ipBan(ipAddress);
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
                source.sendMessage(
                    MiniMessage.miniMessage()
                        .deserialize(
                            "<dark_red><bold>ERROR</bold></dark_red> <red>Failed to IP-ban <ip>!</red>",
                            Placeholder.unparsed("ip", ipAddress)
                        )
                );
                return;
            }

            source.sendMessage(
                MiniMessage.miniMessage()
                    .deserialize(
                        "<gray>You have IP-banned <gold><bold><ip></bold></gold>!</gray>",
                        Placeholder.unparsed("ip", ipAddress)
                    )
            );
        }).schedule();
    }
}
