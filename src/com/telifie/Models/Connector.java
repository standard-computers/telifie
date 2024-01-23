package com.telifie.Models;

import com.telifie.Models.Utilities.*;
import org.bson.Document;
import java.util.UUID;

public class Connector {

    private String id, clientId, access, secret, refresh;
    private String user, userId; //User ID of the user in the connector, not Telifie's User ID
    private int origin;

    public Connector(Document document) {
        if (document != null) {
            this.id = (document.getString("id") != null ? document.getString("id") : null);
            this.access = (document.getString("access") != null ? document.getString("access") : null);
            this.secret = (document.getString("secret") != null ? document.getString("secret") : UUID.randomUUID().toString());
            this.refresh = (document.getString("refresh") != null ? document.getString("refresh_") : null);
            this.user = (document.getString("user") != null ? document.getString("user") : "com.telifie.system.garbage"); //Garbage for collection if no user set
            this.userId = (document.getString("user_id") != null ? document.getString("user_id") : null);
            this.clientId = (document.getString("client_id") != null ? document.getString("client_id") : null);
            this.origin = (document.getInteger("origin") != null ? document.getInteger("origin") : Telifie.epochTime());
        }
    }

    public String getClientId() {
        return clientId;
    }

    public String getAccess() {
        return access;
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

    public String getId() {
        return id;
    }

    public String getSecret() {
        return secret;
    }

    public Connector getConnector(){
        return this;
    }

    @Override
    public String toString() {
        return "{\"id\" : \"" + id + '\"' +
                ", \"client_id\" : \"" + clientId + '\"' +
                ", \"access\" : \"" + access + '\"' +
                ", \"secret\" : \"" + secret + '\"' +
                ", \"refresh\" : \"" + refresh + '\"' +
                ", \"user\" : \"" + user + '\"' +
                ", \"user_id\" : \"" + userId + '\"' +
                ", \"origin\" : " + origin +
                '}';
    }
}