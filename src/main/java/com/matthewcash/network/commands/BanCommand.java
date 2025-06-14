package com.matthewcash.network.commands;

import java.io.IOException;
import java.sql.SQLException;
import java.text.ParseException;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.matthewcash.network.BanManager;
import com.matthewcash.network.PlayerData;
import com.matthewcash.network.NetworkBans;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;

import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;

public class BanCommand implements SimpleCommand {
    @Override
    public boolean hasPermission(final SimpleCommand.Invocation invocation) {
        return invocation.source().hasPermission("mcash.networkbans.ban");
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

            // Check if player is already banned
            try {
                if (BanManager.getBan(playerData) != null) {
                    source.sendMessage(
                        MiniMessage.miniMessage()
                            .deserialize(
                                "<dark_red><bold>ERROR</bold></dark_red> <red>Player <player> is already banned!</red>",
                                Placeholder
                                    .unparsed("player", playerData.username())
                            )
                    );
                    return;
                }
            } catch (SQLException e) {
                NetworkBans.logger.error(
                    "Failed to check ban for " + playerData.username()
                );
                e.printStackTrace();

                source.sendMessage(
                    MiniMessage.miniMessage().deserialize(
                        "<dark_red><bold>ERROR</bold></dark_red> <red>Failed to ban <username>!</red>",
                        Placeholder.unparsed("username", playerData.username())
                    )
                );
                return;
            }

            // Parse reason and time from command args
            String reason;
            TimeFormat timeFormat = null;

            if (args.length < 2) {
                reason = "No Reason Specified";
            } else {
                try {
                    timeFormat = parseDate(args[1]);
                    reason = args.length < 3 ? "No Reason Specified"
                        : String.join(
                            " ",
                            Arrays.copyOfRange(args, 2, args.length)
                        );
                } catch (Exception e) {
                    reason = String.join(
                        " ",
                        Arrays.copyOfRange(args, 1, args.length)
                    );
                }
            }

            // Ban the player
            if (timeFormat == null) {
                try {
                    BanManager.ban(playerData, reason);
                } catch (SQLException e) {
                    NetworkBans.logger.error(
                        "Failed to ban " + playerData.username()
                    );
                    e.printStackTrace();

                    source.sendMessage(
                        MiniMessage.miniMessage().deserialize(
                            "<dark_red><bold>ERROR</bold></dark_red> <red>Failed to ban <username>!</red>",
                            Placeholder
                                .unparsed("username", playerData.username())
                        )
                    );
                    return;
                }

                // Send ban message to sender
                source.sendMessage(
                    MiniMessage.miniMessage()
                        .deserialize(
                            "<gray>You have banned <gold><bold><player></bold></gold> with reason <gold><bold><reason></bold></gold>!</gray>",
                            Placeholder
                                .unparsed("player", playerData.username()),
                            Placeholder.unparsed("reason", reason)
                        )
                );
            } else {
                Instant banUntil = Instant.now()
                    .plusMillis(timeFormat.totalMillis);

                try {
                    BanManager.ban(playerData, reason, banUntil);
                } catch (SQLException e) {
                    NetworkBans.logger.error(
                        "Failed to ban " + playerData.username()
                    );
                    e.printStackTrace();

                    source.sendMessage(
                        MiniMessage.miniMessage().deserialize(
                            "<dark_red><bold>ERROR</bold></dark_red> <red>Failed to ban <username>!</red>",
                            Placeholder
                                .unparsed("username", playerData.username())
                        )
                    );
                    return;
                }

                // Send ban message to sender
                source.sendMessage(
                    MiniMessage.miniMessage()
                        .deserialize(
                            "<gray>You have banned <gold><bold><username></bold></gold> for <gold><bold><time></bold></gold> with reason <gold><bold><reason></bold></gold>!</gray>",
                            Placeholder.unparsed(
                                "time", timeFormat.quantity + " "
                                    + timeFormat.unit
                            ),
                            Placeholder
                                .unparsed("username", playerData.username()),
                            Placeholder.unparsed("reason", reason)
                        )
                );
            }

            Player networkPlayer = NetworkBans.proxy.getPlayer(args[0])
                .orElse(null);

            // Send player to hub
            if (
                networkPlayer != null
                    && !networkPlayer.getCurrentServer().get().getServerInfo()
                        .getName().equals("hub")
            ) {
                networkPlayer.createConnectionRequest(
                    NetworkBans.proxy.getServer("hub").get()
                ).connect();
            }
        }).schedule();

    }

    private static class TimeFormat {
        final String unit;
        final long totalMillis;
        final long quantity;

        TimeFormat(String unit, long totalMillis, long quantity) {
            this.unit = unit;
            this.totalMillis = totalMillis;
            this.quantity = quantity;
        }
    }

    private enum TimeFrame {
        s(1_000, "seconds"), m(60_000, "minutes"), h(3_600_000, "hours"), d(
            86_400_000, "days"
        ), w(
            604_800_000, "weeks"
        ), mo(2_629_746_000L, "months"), y(31_556_952_000L, "years");

        final long millis;
        final String label;

        TimeFrame(long millis, String label) {
            this.millis = millis;
            this.label = label;
        }
    }

    private static TimeFormat parseDate(String input) {
        String[] parts = input.split("(?<=\\D)(?=\\d)|(?<=\\d)(?=\\D)");
        long quantity = Long.parseLong(parts[0]);
        TimeFrame tf = TimeFrame.valueOf(parts[1]);

        return new TimeFormat(tf.label, tf.millis * quantity, quantity);
    }
}
