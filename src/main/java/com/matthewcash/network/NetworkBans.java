package com.matthewcash.network;

import com.matthewcash.network.commands.UnbanCommand;

import java.beans.PropertyVetoException;
import java.io.IOException;
import java.sql.SQLException;

import com.matthewcash.network.commands.BanCommand;
import com.matthewcash.network.commands.CheckCommand;

import net.md_5.bungee.api.plugin.Plugin;

public class NetworkBans extends Plugin {
    private static NetworkBans plugin;

    private DatabaseManager databaseManager;

    public static NetworkBans getPlugin() {
        return plugin;
    }

    public static DatabaseManager getDatabaseManager() {
        return getPlugin().databaseManager;
    }

    @Override
    public void onEnable() {
        plugin = this;

        try {
            databaseManager = new DatabaseManager();
        } catch (IOException | SQLException | PropertyVetoException e) {
            getLogger().severe("Error while occurred initializing database pool!");
            e.printStackTrace();
        }

        databaseManager.generateTable();

        getProxy().getPluginManager().registerListener(this, new SwitchEvent());
        getProxy().getPluginManager().registerCommand(this, new BanCommand());
        getProxy().getPluginManager().registerCommand(this, new UnbanCommand());
        getProxy().getPluginManager().registerCommand(this, new CheckCommand());

        getLogger().info("Enabled NetworkBans!");
    }

    @Override
    public void onDisable() {
        try {
            databaseManager.close();
        } catch (IOException e) {
            getLogger().severe("Error while occurred de-initializing database pool!");
            e.printStackTrace();
        }

        getProxy().getPluginManager().unregisterListener(new SwitchEvent());
        getProxy().getPluginManager().unregisterCommand(new BanCommand());
        getProxy().getPluginManager().unregisterCommand(new UnbanCommand());
        getProxy().getPluginManager().unregisterCommand(new CheckCommand());

        getLogger().info("Disabled NetworkBans!");
    }
}