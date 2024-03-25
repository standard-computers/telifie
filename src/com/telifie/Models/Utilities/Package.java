package com.telifie.Models.Utilities;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Package {

    private String id, name, type, description, access, secret, refresh;
    private int version, origin;
    private boolean isPublic;
    private ArrayList<Endpoint> endpoints;

    @JsonCreator
    public Package(@JsonProperty("id") String id,
                   @JsonProperty("name") String name,
                   @JsonProperty("type") String type,
                   @JsonProperty("description") String description,
                   @JsonProperty("access") String access,
                   @JsonProperty("secret") String secret,
                   @JsonProperty("refresh") String refresh,
                   @JsonProperty("version") int version,
                   @JsonProperty("origin") int origin,
                   @JsonProperty("isPublic") boolean isPublic,
                   @JsonProperty("endpoints") ArrayList<Endpoint> endpoints) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.description = description;
        this.access = access;
        this.secret = secret;
        this.refresh = refresh;
        this.version = version;
        this.origin = origin;
        this.isPublic = isPublic;
        this.endpoints = endpoints;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getAccess() {
        return access;
    }

    public String getSecret() {
        return secret;
    }

    public boolean getPublic() {
        return isPublic;
    }

    @JsonIgnore
    public String getUrl(String name){
        for (Endpoint e : this.endpoints) {
            if (e.name.equals(name)) {
                return e.value;
            }
        }
        return null;
    }

    @JsonIgnore
    public Endpoint getEndpoint(String name){
        for (Endpoint e : this.endpoints) {
            if (e.name.equals(name)) {
                return e;
            }
        }
        return null;
    }

    public String activate(){
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("id", id);
        jsonObject.put("access", access);
        jsonObject.put("secret", secret);
        jsonObject.put("name", name);
        jsonObject.put("type", type);
        jsonObject.put("description", description);
        jsonObject.put("endpoints", new JSONArray(endpoints.toString()));
        return jsonObject.toString();
    }

    @Override
    public String toString() {
        return "{\"id\" : \"" + id + '\"' +
                ", \"name\" : \"" + name + '\"' +
                ", \"type\" : \"" + type + '\"' +
                ", \"isPublic\" : " + isPublic +
                ", \"description\" : \"" + description + "\"}";
    }

    public static class Endpoint {

        protected String name, value, purpose = "";

        @JsonCreator
        public Endpoint(@JsonProperty("name") String name, @JsonProperty("value") String value, @JsonProperty("purpose") String purpose) {
            this.name = name;
            this.value = value;
            this.purpose = purpose;
        }

        @Override
        public String toString() {
            return "{\"name\" : \"" + name + '\"' +
                    ", \"value\" : \"" + value + "\"" +
                    ", \"purpose\" : \"" + purpose + "\"}";
        }
    }
}