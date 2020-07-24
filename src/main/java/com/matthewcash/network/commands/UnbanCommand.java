package com.matthewcash.network.commands;

import java.sql.SQLException;
import java.util.Collections;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;

import com.matthewcash.network.BanManager;
import com.matthewcash.network.BanPlayer;
import com.matthewcash.network.NetworkBans;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.PluginLogger;
import net.md_5.bungee.api.plugin.TabExecutor;

public class UnbanCommand extends Command implements TabExecutor {
    public UnbanCommand() {
        super("unban", "mcash.admin.unban", "pardon");
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

                // Check if Player is banned
                try {
                    if (BanManager.getBan(player) == null) {
                        sender.sendMessage(new ComponentBuilder("ERROR").color(ChatColor.DARK_RED).bold(true)
                                .append(" Player " + player.username + " is not banned!").color(ChatColor.RED)
                                .create());
                        return;
                    }
                } catch (SQLException e) {
                    PluginLogger.getLogger("NetworkBan")
                            .severe("Error occured while checking ban for " + player.username);
                    sender.sendMessage(new ComponentBuilder("ERROR").color(ChatColor.DARK_RED).bold(true)
                            .append(" An error occured while checking ban for " + player.username + "!")
                            .color(ChatColor.RED).create());
                    e.printStackTrace();
                    return;
                }

                // Unban Player
                try {
                    BanManager.unban(player);
                } catch (SQLException e) {
                    PluginLogger.getLogger("NetworkBan").severe("Error occured while unbanning " + player.username);
                    sender.sendMessage(new ComponentBuilder("ERROR").color(ChatColor.DARK_RED).bold(true)
                            .append(" An error occured while unbanning " + player.username + "!").color(ChatColor.RED)
                            .create());
                    e.printStackTrace();
                    return;
                }
                sender.sendMessage(new ComponentBuilder("You unbanned ").color(ChatColor.GRAY).append(player.username)
                        .color(ChatColor.GOLD).bold(true).append("!").color(ChatColor.GRAY).create());

            }
        });

    }
}