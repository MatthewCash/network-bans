package com.matthewcash.network;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;

public class ConfigManager {
    public static CommentedFileConfig config = CommentedFileConfig
        .builder(NetworkBans.dataDirectory.resolve("config.toml").toFile())
        .defaultData(
            NetworkBans.class.getResource(
                "/config.toml"
            )
        )
        .autosave()
        .preserveInsertionOrder()
        .sync()
        .build();

    public static void loadConfig() {
        NetworkBans.dataDirectory.toFile().mkdirs();

        config.load();
    }
}
