package com.matthewcash.network;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.UUID;

import javax.net.ssl.HttpsURLConnection;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.PluginLogger;

public class BanPlayer {
    public final String username;
    public final UUID uuid;

    private BanPlayer(String username, UUID uuid) {
        this.username = username;
        this.uuid = uuid;
    }

    public static BanPlayer getPlayer(String providedName) {
        // Get Connected Player
        ProxiedPlayer player = NetworkBans.getPlugin().getProxy().getPlayer(providedName);
        if (player != null) {
            return new BanPlayer(player.getName(), player.getUniqueId());
        }

        // Fallback to API for offline player
        try {
            // GET request for UUID
            HttpsURLConnection request = (HttpsURLConnection) new URL(
                    "https://api.mojang.com/users/profiles/minecraft/" + providedName).openConnection();
            request.connect();

            // Return null if no UUID
            if (request.getResponseCode() == 204) {
                return null;
            }

            // Read JSON
            BufferedReader in = new BufferedReader(new InputStreamReader(request.getInputStream()));
            String inputLine;
            StringBuffer content = new StringBuffer();
            while ((inputLine = in.readLine()) != null) {
                content.append(inputLine);
            }
            String response = content.toString();

            in.close();
            request.disconnect();

            // Parse JSON and UUID
            JsonObject responseJSON = (JsonObject) new JsonParser().parse(response).getAsJsonObject();

            String verifiedName = responseJSON.get("name").getAsString();

            String responseUUID = responseJSON.get("id").getAsString();
            String dashedUUID = responseUUID.replaceAll("(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})", "$1-$2-$3-$4-$5");

            UUID uuid = UUID.fromString(dashedUUID);

            return new BanPlayer(verifiedName, uuid);

        } catch (IOException e) {
            PluginLogger.getLogger("NetworkBan").severe("HTTP Error getting UUID for " + providedName);
            return null;
        }
    }
}