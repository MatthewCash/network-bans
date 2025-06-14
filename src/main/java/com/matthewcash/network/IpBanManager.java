package com.matthewcash.network;

import java.io.IOException;
import java.io.StringReader;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;

import com.velocitypowered.api.proxy.Player;

public class IpBanManager {
    private static final URI apiUri = URI.create(
        ConfigManager.config
            .get("ipban.api_url")
    );
    private static final String authToken = ConfigManager.config
        .get("ipban.auth_token");

    private static final HttpClient httpClient = HttpClient.newHttpClient();

    public static String getIpFromPlayer(Player player) {
        return player.getRemoteAddress().getAddress().getHostAddress();
    }

    public static boolean isValidIpAddress(String ipAddress) {
        try {
            InetAddress address = InetAddress.getByName(ipAddress);
            return ipAddress.equals(address.getHostAddress());
        } catch (Exception e) {
            return false;
        }
    }

    public static void ipBan(String ipAddress)
        throws IOException, InterruptedException, URISyntaxException {
        String json = Json.createObjectBuilder()
            .add("addr", ipAddress)
            .build()
            .toString();

        HttpRequest request = HttpRequest.newBuilder()
            .uri(new URI(apiUri + "/add"))
            .timeout(Duration.ofSeconds(1))
            .header("Content-Type", "application/json")
            .header("Authorization", authToken)
            .POST(HttpRequest.BodyPublishers.ofString(json))
            .build();

        HttpResponse<String> response = httpClient
            .send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            NetworkBans.logger.error(response.body());
            throw new RuntimeException(
                "Response code is not 200:" + response.body()
            );
        }
    }

    public static void ipUnBan(String ipAddress)
        throws IOException, InterruptedException, URISyntaxException {
        String json = Json.createObjectBuilder()
            .add("addr", ipAddress)
            .build()
            .toString();

        HttpRequest request = HttpRequest.newBuilder()
            .uri(new URI(apiUri + "/remove"))
            .timeout(Duration.ofSeconds(1))
            .header("Content-Type", "application/json")
            .header("Authorization", authToken)
            .POST(HttpRequest.BodyPublishers.ofString(json))
            .build();

        HttpResponse<String> response = httpClient
            .send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            NetworkBans.logger.error(response.body());
            throw new RuntimeException(
                "Response code is not 200:" + response.body()
            );
        }
    }

    public static boolean checkIp(String ipAddress)
        throws IOException, InterruptedException, URISyntaxException {
        String json = Json.createObjectBuilder()
            .add("addr", ipAddress)
            .build()
            .toString();

        HttpRequest request = HttpRequest.newBuilder()
            .uri(new URI(apiUri + "/check"))
            .timeout(Duration.ofSeconds(1))
            .header("Content-Type", "application/json")
            .header("Authorization", authToken)
            .POST(HttpRequest.BodyPublishers.ofString(json))
            .build();

        HttpResponse<String> response = httpClient
            .send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            NetworkBans.logger.error(response.body());
            throw new RuntimeException(
                "Response code is not 200:" + response.body()
            );
        }

        try (
            JsonReader reader = Json
                .createReader(new StringReader(response.body()))
        ) {
            JsonObject jsonObject = reader.readObject();
            return jsonObject.getBoolean("present", false);
        }
    }
}
