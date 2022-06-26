package com.matthewcash.network.commands;

import java.io.IOException;
import java.sql.SQLException;
import java.text.DateFormat;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import com.matthewcash.network.Ban;
import com.matthewcash.network.BanManager;
import com.matthewcash.network.BanPlayer;
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

        return NetworkBans.proxy.getAllPlayers().stream().map(player -> player.getUsername()).toList();
    }

    @Override
    public void execute(final SimpleCommand.Invocation invocation) {
        CommandSource source = invocation.source();
        String[] args = invocation.arguments();

        if (args.length < 1) {
            source.sendMessage(MiniMessage.miniMessage()
                .deserialize("<dark_red><bold>ERROR</bold></dark_red> <red>You must specify a player!</red>"));
            return;
        }

        NetworkBans.proxy.getScheduler().buildTask(NetworkBans.plugin, () -> {
            String ipAddress = null;
            BanPlayer player = BanPlayer.getPlayer(args[0]);

            if (player != null) {
                Optional<Player> proxiedPlayer = NetworkBans.proxy.getPlayer(args[0]);
                if (proxiedPlayer.isPresent()) {
                    ipAddress = IpBanManager.getIpFromPlayer(proxiedPlayer.get());
                }
            } else {
                ipAddress = args[0];
            }

            if (ipAddress != null && !IpBanManager.isValidIpAddress(ipAddress)) {
                ipAddress = null;
            }

            if (player == null && ipAddress == null) {
                source.sendMessage(MiniMessage.miniMessage().deserialize(
                    "<dark_red><bold>ERROR</bold></dark_red> <red>You must specify a valid player or IP address!</red>"));
                return;
            }

            // Check Ban
            Ban ban = null;
            if (player != null) {
                try {
                    ban = BanManager.getBan(player);
                } catch (SQLException e) {
                    NetworkBans.logger.error("Error occurred while checking ban for " + player.username);
                    source.sendMessage(MiniMessage.miniMessage().deserialize(
                        "<dark_red><bold>ERROR</bold></dark_red> <red>An error occurred while checking ban for <username>!</red>",
                        Placeholder.unparsed("username", player.username)));
                    e.printStackTrace();
                    return;
                }
            }

            // Check IP-Ban
            boolean isIpBanned = false;
            if (ipAddress != null) {
                try {
                    isIpBanned = IpBanManager.checkIp(ipAddress);
                } catch (IOException | RuntimeException e) {
                    NetworkBans.logger.error("Error occurred while checking IP-Ban for " + ipAddress);
                    e.printStackTrace();

                    source.sendMessage(MiniMessage.miniMessage().deserialize(
                        "<dark_red><bold>ERROR</bold></dark_red> <red>An error occurred while checking IP-ban for <ip>!</red>",
                        Placeholder.unparsed("ip", ipAddress)));
                }
            }

            // Not Banned
            if (ban == null && isIpBanned == false) {
                String name = player != null ? player.username : ipAddress;

                source.sendMessage(MiniMessage.miniMessage()
                    .deserialize("<bold><green>✓</green> <gold><name></gold></bold> <gray>is not banned</gray>",
                        Placeholder.unparsed("name", name)));
                return;
            }

            if (isIpBanned) {
                source.sendMessage(MiniMessage.miniMessage().deserialize(
                    "<bold><red>✖</red> <gold><ip></gold></bold> <gray>has been <red><bold>IP-BANNED</bold></red>!</gray>",
                    Placeholder.unparsed("ip", ipAddress)));
            }

            if (ban != null) {
                Component banComponent = MiniMessage.miniMessage().deserialize(
                    "<bold><red>✖</red> <gold><username></gold></bold> <gray>has been <red><bold>BANNED</bold></red> with reason <gold><bold><reason></bold></gold></gray>",
                    Placeholder.unparsed("username", player.username), Placeholder.unparsed("reason", ban.reason));

                if (ban.unbanTime == null) {
                    source.sendMessage(banComponent.append(Component.text("!").color(NamedTextColor.GRAY)));
                    return;
                }

                source.sendMessage(banComponent.append(MiniMessage.miniMessage().deserialize(
                    "<gray> until <gold><bold><time></bold></gold>!</gray>", Placeholder.unparsed("time",
                        DateFormat.getDateTimeInstance().format(ban.unbanTime)))));

            }
        })
            .schedule();

    }
}