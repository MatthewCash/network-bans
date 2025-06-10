package com.matthewcash.network;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.velocitypowered.api.proxy.Player;

public record PlayerData(String username, UUID uuid, String ipAddress) {

    public static PlayerData getPlayer(String providedName)
        throws InterruptedException {
        Optional<Player> player = NetworkBans.proxy.getPlayer(providedName);
        if (player.isPresent()) {
            return new PlayerData(
                player.get().getUsername(),
                player.get().getUniqueId(),
                player.get().getRemoteAddress().getAddress().toString()
            );
        }

        // Fallback to API for offline player
        try {
            URI uri = URI.create(
                "https://api.mojang.com/users/profiles/minecraft/"
                    + providedName
            );
            HttpRequest request = HttpRequest.newBuilder()
                .uri(uri)
                .timeout(Duration.ofSeconds(1))
                .GET()
                .build();

            HttpClient client = HttpClient.newHttpClient();
            HttpResponse<String> response = client
                .send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 204)
                return null;

            JsonObject responseJSON = JsonParser.parseString(response.body())
                .getAsJsonObject();
            String verifiedName = responseJSON.get("name").getAsString();
            String responseUUID = responseJSON.get("id").getAsString();
            String dashedUUID = responseUUID.replaceAll(
                "(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})", "$1-$2-$3-$4-$5"
            );
            UUID uuid = UUID.fromString(dashedUUID);

            return new PlayerData(verifiedName, uuid, null);

        } catch (IOException e) {
            NetworkBans.logger
                .error("HTTP Error getting UUID for " + providedName, e);
            return null;
        }
    }
}
