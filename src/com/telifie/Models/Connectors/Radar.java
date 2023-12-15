package com.telifie.Models.Connectors;

import com.telifie.Models.Utilities.Packages;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

public class Radar {
    public static JSONObject get(String address) throws IOException, InterruptedException {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.radar.io/v1/geocode/forward?query=" + URLEncoder.encode(address, StandardCharsets.UTF_8.toString())))
                .header("Authorization", Packages.get("com.telifie.connectors.radar").getAccess())
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        return new JSONObject(response.body()).getJSONArray("addresses").getJSONObject(0);
    }
}
