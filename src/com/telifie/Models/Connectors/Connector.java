package com.telifie.Models.Connectors;

import com.telifie.Models.Authentication;
import com.telifie.Models.Utilities.*;
import java.io.Serializable;
import java.util.ArrayList;

public class Connector extends CommonObject implements Serializable {

    private String client, token, refreshToken; //Data from API host, use their backend/platform
    /**
     * For CommonObject with connectors, use title for name, and link for endpoint url
     * */
    private ArrayList<CommonObject> endpoints = new ArrayList<>();
    private Authentication authentication; //The authentication with Telifie to use this Connector, replicated when in use

    public Connector(){
        super("", "", "", "");
    }

    public Connector(String icon, String title, String link, String description) {
        super(icon, title, link, description);
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

    public ArrayList<CommonObject> getEndpoints() {
        return endpoints;
    }

    public void setEndpoints(ArrayList<CommonObject> endpoints) {
        this.endpoints = endpoints;
    }

    public void addEndpoint(CommonObject object){

    }

    public Authentication getAuthentication() {
        return authentication;
    }

    public void setAuthentication(Authentication authentication) {
        this.authentication = authentication;
    }

}
