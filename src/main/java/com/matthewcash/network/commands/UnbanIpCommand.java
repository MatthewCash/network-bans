package com.matthewcash.network.commands;

import java.io.IOException;
import java.util.Collections;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.matthewcash.network.IpBanManager;
import com.matthewcash.network.NetworkBans;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.TabExecutor;

public class UnbanIpCommand extends Command implements TabExecutor {
    public UnbanIpCommand() {
        super("unbanip", "mcash.admin.unbanip", "ipunban", "unipban");
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
                    .append(" You must specify an online player or IP address!").color(ChatColor.RED).create());
            return;
        }

        String ipAddress;

        ProxiedPlayer player = ProxyServer.getInstance().getPlayer(args[0]);
        if (player != null) {
            ipAddress = IpBanManager.getIpFromPlayer(player);
        } else {
            ipAddress = args[0];
        }

        if (!IpBanManager.isValidIpAddress(ipAddress)) {
            sender.sendMessage(new ComponentBuilder("ERROR").color(ChatColor.DARK_RED).bold(true)
                    .append(" You must specify an online valid player or IP address!").color(ChatColor.RED).create());
            return;
        }

        NetworkBans.getPlugin().getProxy().getScheduler().runAsync(NetworkBans.getPlugin(), new Runnable() {
            @Override
            public void run() {

                try {
                    if (!IpBanManager.checkIp(ipAddress)) {
                        sender.sendMessage(new ComponentBuilder("ERROR").color(ChatColor.DARK_RED).bold(true)
                                .append(" IP Address " + ipAddress + " is not banned!").color(ChatColor.RED)
                                .create());
                        return;
                    }

                    IpBanManager.ipUnBan(ipAddress);
                } catch (IOException | RuntimeException e) {
                    sender.sendMessage(new ComponentBuilder("ERROR").color(ChatColor.DARK_RED).bold(true)
                            .append(" An error occurred while IP-Banning " + ipAddress + "!").color(ChatColor.RED)
                            .create());
                    return;
                }

                sender.sendMessage(new ComponentBuilder("You have IP-Unbanned ").color(ChatColor.GRAY)
                        .append(ipAddress).color(ChatColor.GOLD).bold(true).append("!")
                        .color(ChatColor.GRAY).create());
            }
        });
    }
}
