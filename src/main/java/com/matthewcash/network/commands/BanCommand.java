package com.matthewcash.network.commands;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;

import com.matthewcash.network.BanManager;
import com.matthewcash.network.NetworkBans;
import com.matthewcash.network.BanPlayer;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.PluginLogger;
import net.md_5.bungee.api.plugin.TabExecutor;

public class BanCommand extends Command implements TabExecutor {
    public BanCommand() {
        super("ban", "mcash.admin.ban", new String[0]);
    }

    @Override
    public Iterable<String> onTabComplete(CommandSender sender, String[] args) {
        if (args.length != 1) {
            return Collections.emptyList();
        }
        return Iterables
                .transform(Iterables.filter(ProxyServer.getInstance().getPlayers(), new Predicate<ProxiedPlayer>() {
                    @Override
                    public boolean apply(ProxiedPlayer player) {
                        return player.getName().toLowerCase().startsWith(args[0]);
                    }
                }), new Function<ProxiedPlayer, String>() {
                    @Override
                    public String apply(ProxiedPlayer player) {
                        return player.getName();
                    }
                });
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length < 1) {
            sender.sendMessage(new ComponentBuilder("ERROR").color(ChatColor.DARK_RED).bold(true)
                    .append(" You must specify a player!").color(ChatColor.RED).create());
            return;
        }

        NetworkBans.getPlugin().getProxy().getScheduler().runAsync(NetworkBans.getPlugin(), new Runnable() {
            @Override
            public void run() {
                // Lookup UUID
                BanPlayer player = BanPlayer.getPlayer(args[0]);

                if (player == null) {
                    sender.sendMessage(new ComponentBuilder("ERROR").color(ChatColor.DARK_RED).bold(true)
                            .append(" Cannot find player " + args[0] + "!").color(ChatColor.RED).create());
                    return;
                }

                // Check if player is already banned
                try {
                    if (BanManager.getBan(player) != null) {
                        sender.sendMessage(new ComponentBuilder("ERROR").color(ChatColor.DARK_RED).bold(true)
                                .append(" Player " + player.username + " is already banned!").color(ChatColor.RED)
                                .create());
                        return;
                    }
                } catch (SQLException e) {
                    PluginLogger.getLogger("NetworkBans")
                            .severe("Error occurred while checking ban for " + player.username);
                    sender.sendMessage(new ComponentBuilder("ERROR").color(ChatColor.DARK_RED).bold(true)
                            .append(" An error occurred while checking ban for " + player.username + "!")
                            .color(ChatColor.RED).create());
                    e.printStackTrace();
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
                        reason = arrayToString(Arrays.copyOfRange(args, 1, args.length - 1), " ");
                    } catch (Exception e) {
                        reason = arrayToString(Arrays.copyOfRange(args, 1, args.length), " ");
                    }
                }

                // Ban the player
                if (timeFormat == null) {
                    try {
                        BanManager.ban(player, reason);
                    } catch (SQLException e) {
                        PluginLogger.getLogger("NetworkBans").severe("Error occurred while banning " + player.username);
                        sender.sendMessage(new ComponentBuilder("ERROR").color(ChatColor.DARK_RED).bold(true)
                                .append(" An error occurred while banning " + player.username + "!")
                                .color(ChatColor.RED)
                                .create());
                        e.printStackTrace();
                        return;
                    }

                    // Send ban message to sender
                    sender.sendMessage(new ComponentBuilder("You have banned ").color(ChatColor.GRAY)
                            .append(player.username).color(ChatColor.GOLD).bold(true).append(" with reason ")
                            .color(ChatColor.GRAY).append(reason).color(ChatColor.GOLD).bold(true).append("!")
                            .color(ChatColor.GRAY).create());
                } else {
                    Date banUntil = new Date(new Date().getTime() + timeFormat.totalTime);

                    try {
                        BanManager.ban(player, reason, banUntil);
                    } catch (SQLException e) {
                        PluginLogger.getLogger("NetworkBans").severe("Error occurred while banning " + player.username);
                        sender.sendMessage(new ComponentBuilder("ERROR").color(ChatColor.DARK_RED).bold(true)
                                .append(" An error occurred while banning " + player.username + "!")
                                .color(ChatColor.RED)
                                .create());
                        e.printStackTrace();
                        return;
                    }

                    // Send ban message to sender
                    sender.sendMessage(
                            new ComponentBuilder("You have banned ").color(ChatColor.GRAY).append(player.username)
                                    .color(ChatColor.GOLD).bold(true).append(" for ").color(ChatColor.GRAY)
                                    .append(timeFormat.multiplier.toString() + " " + timeFormat.timeFormat)
                                    .color(ChatColor.GOLD).bold(true).append(" with reason ").color(ChatColor.GRAY)
                                    .append(reason).color(ChatColor.GOLD).bold(true).append("!").color(ChatColor.GRAY)
                                    .create());
                }

                ProxiedPlayer networkPlayer = NetworkBans.getPlugin().getProxy().getPlayer(args[0]);

                // Send player to hub
                if (networkPlayer != null && !networkPlayer.getServer().getInfo().getName().equals("hub")) {
                    networkPlayer.connect(NetworkBans.getPlugin().getProxy().getServers().get("hub"));
                }
            }
        });

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
        s(1000l, "seconds"), m(60000l, "minutes"), h(3600000l, "hours"), d(86400000l, "days"), w(604800000l, "weeks"),
        mo(2629746000l, "months"), y(31556952000l, "years");

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