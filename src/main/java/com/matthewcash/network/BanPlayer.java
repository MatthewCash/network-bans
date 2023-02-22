package com.matthewcash.network;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Optional;
import java.util.UUID;

import javax.net.ssl.HttpsURLConnection;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.velocitypowered.api.proxy.Player;

public class BanPlayer {
    public final String username;
    public final UUID uuid;
    public final String ipAddress;

    private BanPlayer(String username, UUID uuid, String ipAddress) {
        this.username = username;
        this.uuid = uuid;
        this.ipAddress = ipAddress;
    }

    public static BanPlayer getPlayer(String providedName) {
        // Get Connected Player
        Optional<Player> player = NetworkBans.proxy.getPlayer(providedName);
        if (player.isPresent()) {
            return new BanPlayer(
                player.get().getUsername(), player.get().getUniqueId(),
                player.get().getRemoteAddress().getAddress().toString()
            );
        }

        // Fallback to API for offline player
        try {
            // GET request for UUID
            HttpsURLConnection request = (HttpsURLConnection) new URL(
                "https://api.mojang.com/users/profiles/minecraft/"
                    + providedName
            ).openConnection();
            request.connect();

            // Return null if no UUID
            if (request.getResponseCode() == 204) {
                return null;
            }

            // Read JSON
            BufferedReader in = new BufferedReader(
                new InputStreamReader(request.getInputStream())
            );
            String inputLine;
            StringBuffer content = new StringBuffer();
            while ((inputLine = in.readLine()) != null) {
                content.append(inputLine);
            }
            String response = content.toString();

            in.close();
            request.disconnect();

            // Parse JSON and UUID
            JsonObject responseJSON = JsonParser.parseString(response)
                .getAsJsonObject();

            String verifiedName = responseJSON.get("name").getAsString();

            String responseUUID = responseJSON.get("id").getAsString();
            String dashedUUID = responseUUID.replaceAll(
                "(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})", "$1-$2-$3-$4-$5"
            );

            UUID uuid = UUID.fromString(dashedUUID);

            return new BanPlayer(verifiedName, uuid, null);

        } catch (IOException e) {
            NetworkBans.logger
                .error("HTTP Error getting UUID for " + providedName);
            return null;
        }
    }
}
