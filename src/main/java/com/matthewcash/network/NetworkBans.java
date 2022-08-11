package com.matthewcash.network;

import java.beans.PropertyVetoException;
import java.io.IOException;
import java.nio.file.Path;
import java.sql.SQLException;

import org.slf4j.Logger;

import com.google.inject.Inject;
import com.matthewcash.network.commands.BanCommand;
import com.matthewcash.network.commands.BanIpCommand;
import com.matthewcash.network.commands.CheckCommand;
import com.matthewcash.network.commands.UnbanCommand;
import com.matthewcash.network.commands.UnbanIpCommand;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;

public class NetworkBans {
    public static NetworkBans plugin;
    public static ProxyServer proxy;
    public static Logger logger;
    public static Path dataDirectory;

    public static DatabaseManager databaseManager;

    @Inject
    public NetworkBans(ProxyServer proxy, Logger logger, @DataDirectory Path dataDirectory) {
        NetworkBans.plugin = this;
        NetworkBans.proxy = proxy;
        NetworkBans.logger = logger;
        NetworkBans.dataDirectory = dataDirectory;
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        ConfigManager.loadConfig();

        try {
            databaseManager = new DatabaseManager();
        } catch (IOException | SQLException | PropertyVetoException e) {
            logger.error("Error while occurred initializing database pool!");
            e.printStackTrace();
        }

        databaseManager.generateTable();

        proxy.getEventManager().register(this, new SwitchEvent());
        proxy.getCommandManager().register("ban", new BanCommand(), "punish");
        proxy.getCommandManager().register("unban", new UnbanCommand(), "pardon");
        proxy.getCommandManager().register("check", new CheckCommand(), "checkban");
        proxy.getCommandManager().register("banip", new BanIpCommand(), "ipban");
        proxy.getCommandManager().register("unbanip", new UnbanIpCommand(), "unipban", "ipunban", "pardonip",
            "ippardon");

        logger.info("Enabled NetworkBans!");
    }
}