package com.telifie.Models.Connectors;

import com.telifie.Models.Utilities.*;
import org.bson.Document;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.UUID;

public class Connector implements Serializable {

    private String id, name, client, token, refreshToken;
    private ArrayList<Endpoint> endpoints = new ArrayList<>();

    public Connector(Document document){

        if((this.client = document.getString("client")) != null
                && (this.token = document.getString("token")) != null
                && (this.name = document.getString("name")) != null
                && (this.refreshToken = document.getString("refresh_token")) != null) {

            this.id = (document.getString("id") != null ? document.getString("id") : UUID.randomUUID().toString());
            ArrayList<Document> eps = (ArrayList<Document>) document.getList("endpoints", Document.class);
            if (eps != null) {

                endpoints = new ArrayList<>();
                for (Document d : eps) {

                    endpoints.add(new Endpoint(d));
                }
            }
        }
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

    public String getClient() {
        return client;
    }

    public void setClient(String client) {
        this.client = client;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public ArrayList<Endpoint> getEndpoints() {
        return endpoints;
    }

    public void setEndpoints(ArrayList<Endpoint> endpoints) {
        this.endpoints = endpoints;
    }

    public void addEndpoint(Endpoint endpoint) {
        this.endpoints.add(endpoint);
    }

    @Override
    public String toString() {
        return "{\"id\" : \"" + id + '\"' +
                ", \"name\" : \"" + name + '\"' +
                ", \"client\" : \"" + client + '\"' +
                ", \"token\" : \"" + token + '\"' +
                ", \"refresh_token\" : \"" + refreshToken + '\"' +
                ", \"endpoints\" : " + endpoints +
                '}';
    }

}
