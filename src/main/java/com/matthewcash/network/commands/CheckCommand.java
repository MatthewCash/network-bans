package com.matthewcash.network.commands;

import java.sql.SQLException;
import java.text.DateFormat;
import java.util.Collections;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;

import com.matthewcash.network.Ban;
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

public class CheckCommand extends Command implements TabExecutor {
    public CheckCommand() {
        super("check", "mcash.admin.check", new String[0]);
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

                // Check Ban
                Ban ban = null;
                try {
                    ban = BanManager.getBan(player);
                } catch (SQLException e) {
                    PluginLogger.getLogger("NetworkBan")
                            .severe("Error occured while checking ban for " + player.username);
                    sender.sendMessage(new ComponentBuilder("ERROR").color(ChatColor.DARK_RED).bold(true)
                            .append(" An error occured while checking ban for " + player.username + "!")
                            .color(ChatColor.RED).create());
                    e.printStackTrace();
                    return;
                }

                // Send message
                if (ban == null) {
                    sender.sendMessage(new ComponentBuilder("✓").color(ChatColor.GREEN).bold(true)
                            .append(" " + player.username).color(ChatColor.GOLD).bold(true).append(" is not banned!")
                            .color(ChatColor.GRAY).create());
                    return;
                }
                ComponentBuilder banMessage = new ComponentBuilder("✖").color(ChatColor.RED)
                        .append(" " + player.username).color(ChatColor.GOLD).bold(true).append(" has been ")
                        .color(ChatColor.GRAY).append("BANNED").color(ChatColor.RED).bold(true).append(" with reason ")
                        .color(ChatColor.GRAY).append(ban.reason).color(ChatColor.GOLD).bold(true).append("!")
                        .color(ChatColor.GRAY);

                if (ban.unbanTime == null) {
                    sender.sendMessage(banMessage.create());
                    return;
                }
                sender.sendMessage(banMessage.append(" until ").color(ChatColor.GRAY)
                        .append(DateFormat.getDateTimeInstance().format(ban.unbanTime)).color(ChatColor.GOLD).bold(true)
                        .append("!").color(ChatColor.GRAY).create());
            }
        });

    }
}