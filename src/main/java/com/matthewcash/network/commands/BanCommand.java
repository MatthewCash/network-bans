package com.matthewcash.network.commands;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
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
            } catch (InterruptedException e) {
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
                    timeFormat = parseDate(args[args.length - 1]);
                    reason = arrayToString(
                        Arrays.copyOfRange(args, 1, args.length - 1), " "
                    );
                } catch (Exception e) {
                    reason = arrayToString(
                        Arrays.copyOfRange(args, 1, args.length), " "
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
                Date banUntil = new Date(
                    new Date().getTime() + timeFormat.totalTime
                );

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
                                "time", timeFormat.multiplier.toString() + " "
                                    + timeFormat.timeFormat
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

    private static String arrayToString(String[] array, String separator) {
        StringBuilder sb = new StringBuilder();
        for (String string : array) {
            sb.append(string).append(separator);
        }
        return sb.substring(0, sb.length() - 1);
    }

    private static class TimeFormat {
        String timeFormat;
        Long totalTime;
        Long multiplier;

        public TimeFormat(String timeFormat, Long totalTime, Long multiplier) {
            this.timeFormat = timeFormat;
            this.totalTime = totalTime;
            this.multiplier = multiplier;
        }
    }

    private static enum TimeFrame {
        s(1000l, "seconds"), m(60000l, "minutes"), h(3600000l, "hours"), d(
            86400000l, "days"
        ), w(
            604800000l,
            "weeks"
        ), mo(2629746000l, "months"), y(31556952000l, "years");

        private final Long totalTime;
        private final String timeFormat;

        private TimeFrame(Long totalTime, String timeFormat) {
            this.totalTime = totalTime;
            this.timeFormat = timeFormat;
        }
    }

    private static TimeFormat parseDate(String input) throws Exception {
        String[] groups = input.split("(?<=\\D)(?=\\d)|(?<=\\d)(?=\\D)");
        Long multiplier = Long.parseLong(groups[0]);
        String timeString = groups[1];

        TimeFrame timeFrame = TimeFrame.valueOf(timeString);

        Long totalTime = timeFrame.totalTime * multiplier;
        String timeFormat = (String) timeFrame.timeFormat;

        return new TimeFormat(timeFormat, totalTime, multiplier);
    }

}
