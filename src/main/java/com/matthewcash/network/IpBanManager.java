package com.matthewcash.network;

import java.io.IOException;
import java.io.StringReader;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonReader;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;

import com.velocitypowered.api.proxy.Player;

public class IpBanManager {
    private static final String apiPath = ConfigManager.config.get("ipban.api_url");
    private static final String authToken = ConfigManager.config.get("ipban.auth_token");

    public static String getIpFromPlayer(Player player) {
        return player.getRemoteAddress().getAddress().toString();
    }

    public static boolean isValidIpAddress(String ipAddress) {
        return ipAddress.matches("^(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$");
    }

    public static void ipBan(String ipAddress) throws ClientProtocolException, IOException {
        JsonObjectBuilder payloadJsonBuilder = Json.createObjectBuilder();
        payloadJsonBuilder.add("ban", ipAddress);
        String rawJson = payloadJsonBuilder.build().toString();

        HttpClient httpClient = HttpClientBuilder.create().build();
        HttpResponse response;

        HttpPost request = new HttpPost(apiPath);

        StringEntity params = new StringEntity(rawJson);
        request.addHeader("content-type", "application/json");
        request.addHeader("Authorization", authToken);

        request.setEntity(params);
        response = httpClient.execute(request);

        if (response.getStatusLine().getStatusCode() != 200) {
            NetworkBans.logger.error(response.getStatusLine().getReasonPhrase());
            throw new RuntimeException("An error occurred while attempting to IP-Ban!");
        }
    }

    public static void ipUnBan(String ipAddress) throws ClientProtocolException, IOException {
        JsonObjectBuilder payloadJsonBuilder = Json.createObjectBuilder();
        payloadJsonBuilder.add("unban", ipAddress);
        String rawJson = payloadJsonBuilder.build().toString();

        HttpClient httpClient = HttpClientBuilder.create().build();
        HttpResponse response;

        HttpPost request = new HttpPost(apiPath);

        StringEntity params = new StringEntity(rawJson);
        request.addHeader("content-type", "application/json");
        request.addHeader("Authorization", authToken);

        request.setEntity(params);
        response = httpClient.execute(request);

        if (response.getStatusLine().getStatusCode() != 200) {
            NetworkBans.logger.error(response.getStatusLine().getReasonPhrase());
            throw new RuntimeException("An error occurred while attempting to IP-UnBan!");
        }
    }

    public static boolean checkIp(String ipAddress) throws ClientProtocolException, IOException {
        JsonObjectBuilder payloadJsonBuilder = Json.createObjectBuilder();
        payloadJsonBuilder.add("check", ipAddress);
        String rawJson = payloadJsonBuilder.build().toString();

        HttpClient httpClient = HttpClientBuilder.create().build();
        HttpResponse response;

        HttpPost request = new HttpPost(apiPath);

        StringEntity params = new StringEntity(rawJson);
        request.addHeader("content-type", "application/json");
        request.addHeader("Authorization", authToken);

        request.setEntity(params);
        response = httpClient.execute(request);

        if (response.getStatusLine().getStatusCode() != 200) {
            NetworkBans.logger.error(response.getStatusLine().getReasonPhrase());
            throw new RuntimeException("An error occurred while checking for IP-Ban!");
        }

        String responseBody = EntityUtils.toString(response.getEntity());

        JsonReader jsonReader = Json.createReader(new StringReader(responseBody));
        JsonObject jsonMessage = jsonReader.readObject();

        return jsonMessage.getBoolean("isIpBanned");
    }
}
