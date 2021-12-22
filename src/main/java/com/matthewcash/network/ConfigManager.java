package com.matthewcash.network;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;

public class ConfigManager {
    public static Configuration config;

    public static void loadConfig() throws IOException {
        File configFile = new File(NetworkBans.getPlugin().getDataFolder(), "config.yml");

        // Create config file if it does not exist
        if (!configFile.exists()) {
            createConfig(configFile);
        }

        config = ConfigurationProvider.getProvider(YamlConfiguration.class)
                .load(configFile);
    }

    private static void createConfig(File configFile) throws IOException {
        configFile.getParentFile().mkdirs();

        InputStream configStream = NetworkBans.getPlugin().getResourceAsStream("config.yml");
        Files.copy(configStream, configFile.toPath());
    }

    public static Configuration getConfig() {
        return config;
    }
}
