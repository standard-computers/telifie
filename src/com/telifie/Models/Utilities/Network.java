package com.telifie.Models.Utilities;

import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

public class Network {

    private String url;
    private int statusCode;

    public HttpResponse get(String url){
        this.url = url;
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request;
        try {
            request = HttpRequest.newBuilder()
                    .version(HttpClient.Version.HTTP_1_1)
                    .headers("User-Agent", "Telifie")
                    .uri(new URI(url))
                    .GET()
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            this.statusCode = response.statusCode();
            return response;
        } catch (IOException | InterruptedException | URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    public CloseableHttpResponse post(String url, List<NameValuePair> params){
        CloseableHttpClient httpClient = HttpClients.createDefault();
        try {
            HttpPost request = new HttpPost(url);
            request.setEntity(new UrlEncodedFormEntity(params));
            CloseableHttpResponse response = httpClient.execute(request);
            this.statusCode = response.getStatusLine().getStatusCode();
            httpClient.close();
            return response;
        } catch (Exception ignored) {
        }
        return null;
    }

    public String getUrl() {
        return url;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public void crawl(){

    }

}
