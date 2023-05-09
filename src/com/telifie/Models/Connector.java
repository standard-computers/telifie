package com.telifie.Models;

import com.telifie.Models.Utilities.*;
import org.bson.Document;
import java.util.ArrayList;
import java.util.UUID;

public class Connector {

    private String id, name, clientId, accessToken, secret, refreshToken, redirectUri;
    private String user, userId; //User ID of the user in the connector, not Telifie's User ID
    private ArrayList<Endpoint> endpoints = new ArrayList<>();
    private int origin;

    public Connector(Document document) {
        if (document != null) {
            this.id = (document.getString("id") != null ? document.getString("id") : null);
            this.name = (document.getString("name") != null ? document.getString("name") : null);
            this.accessToken = (document.getString("access_token") != null ? document.getString("access_token") : null);
            this.secret = (document.getString("secret") != null ? document.getString("secret") : UUID.randomUUID().toString());
            this.refreshToken = (document.getString("refresh_token") != null ? document.getString("refresh_token") : null);
            this.user = (document.getString("user") != null ? document.getString("user") : "com.telifie.system.garbage"); //Garbage for collection if no user set
            this.userId = (document.getString("user_id") != null ? document.getString("user_id") : null);
            this.clientId = (document.getString("client_id") != null ? document.getString("client_id") : null);
            this.redirectUri = (document.getString("redirect_uri") != null ? document.getString("redirect_uri") : "");
            this.origin = (document.getInteger("origin") != null ? document.getInteger("origin") : Telifie.getEpochTime());
            ArrayList<Document> eps = (ArrayList<Document>) document.getList("endpoints", Document.class);
            if (eps != null) {
                endpoints = new ArrayList<>();
                for (Document d : eps) {
                    endpoints.add(new Endpoint(d));
                }
            }
        }
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public ArrayList<Endpoint> getEndpoints() {
        return endpoints;
    }

    public void setEndpoints(ArrayList<Endpoint> endpoints) {
        this.endpoints = endpoints;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSecret() {
        return secret;
    }

    public String getRedirectUri() {
        return redirectUri;
    }

    public Connector getConnector(){
        return this;
    }

    @Override
    public String toString() {
        return "{\"id\" : \"" + id + '\"' +
                ", \"name\" : \"" + name + '\"' +
                ", \"client_id\" : \"" + clientId + '\"' +
                ", \"access_token\" : \"" + accessToken + '\"' +
                ", \"secret\" : \"" + secret + '\"' +
                ", \"refresh_token\" : \"" + refreshToken + '\"' +
                ", \"redirect_uri\" : \"" + redirectUri + '\"' +
                ", \"user\" : \"" + user + '\"' +
                ", \"user_id\" : \"" + userId + '\"' +
                ", \"endpoints\" : " + endpoints +
                ", \"origin\" : " + origin +
                '}';
    }
}
