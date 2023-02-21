package com.telifie.Models.Connectors;

import com.telifie.Models.Authentication;
import com.telifie.Models.Utilities.*;
import java.io.Serializable;
import java.util.ArrayList;

public class Connector implements Serializable {

    private String id, client, token, refreshToken;
    private ArrayList<Endpoint> endpoints = new ArrayList<>();
    private Authentication authentication; //The authentication with Telifie to use this Connector, replicated when in use

    public String getId() {
        return id;
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

    public Authentication getAuthentication() {
        return authentication;
    }

    public void setAuthentication(Authentication authentication) {
        this.authentication = authentication;
    }

    class Endpoint {

        private String url, method, description;

        public Endpoint(String url, String method, String description) {
            this.url = url;
            this.method = method;
            this.description = description;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getMethod() {
            return method;
        }

        public void setMethod(String method) {
            this.method = method;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        @Override
        public String toString() {
            return "{" +
                    "\"url\" : \"" + url + '\"' +
                    ", \"method\" :" + method + '\"' +
                    ", \"description\" :" + method + '\"' +
                    '}';
        }
    }

}
