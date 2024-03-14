package com.telifie.Models.Utilities;

import org.bson.Document;
import org.json.JSONObject;

import java.util.HashMap;

public class Package {

    private final String id, name, type, icon, description, access, secret, refresh;
    private int version;
    private final int origin;
    private HashMap<String, String> urls = new HashMap<>();

    public Package(Document document){
        this.id = document.getString("id");
        this.name = document.getString("name");
        this.type = document.getString("type");
        this.icon = document.getString("icon");
        this.description = document.getString("description");
        this.access = document.getString("access");
        this.secret = document.getString("secret");
        this.refresh = document.getString("refresh");
        this.version = (document.getInteger("version") == null ? 1 : document.getInteger("version"));
        this.origin = (document.getInteger("origin") == null ? Telifie.epochTime() : document.getInteger("origin"));
        Document us = document.get("urls", org.bson.Document.class);
        if (us != null) {
            for (String key : us.keySet()) {
                if (us.get(key) instanceof String) {
                    this.urls.put(key, us.getString(key));
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

    public String getAccess() {
        return access;
    }

    public String getSecret() {
        return secret;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public String getUrl(String name){
        return urls.get(name);
    }

    public String activate(){
        JSONObject urlsJson = new JSONObject(urls);
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("id", id);
        jsonObject.put("urls", urlsJson);
        jsonObject.put("type", type);
        jsonObject.put("access", access);
        jsonObject.put("secret", secret);
        jsonObject.put("name", name);
        jsonObject.put("icon", icon);
        jsonObject.put("description", description);
        return jsonObject.toString();
    }

    @Override
    public String toString() {
        return "{" +
                "\"id\" : \"" + id + '\"' +
                ", \"name\" : \"" + name + '\"' +
                ", \"icon\" : \"" + icon + '\"' +
                ", \"description\" : \"" + description + '\"' +
                '}';
    }
}
