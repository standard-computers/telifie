package com.telifie.Models.Utilities;

import org.bson.Document;
import java.util.HashMap;

public class Package {

    private final String id, name, access, secret, refresh;
    private final int version;
    private final int origin;
    private HashMap<String, String> urls;

    public Package(Document document){
        this.id = document.getString("id");
        this.name = document.getString("name");
        this.access = document.getString("access");
        this.secret = document.getString("secret");
        this.refresh = document.getString("refresh");
        this.version = document.getInteger("version");
        this.origin = (document.getInteger("origin") == null ? Telifie.epochTime() : document.getInteger("origin"));
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

    public String getRefresh() {
        return refresh;
    }

    public int getVersion() {
        return version;
    }

    public int getOrigin() {
        return origin;
    }

    public void addUrl(String name, String url){
        urls.put(name, url);
    }

    public boolean hasUrl(String name){
        return urls.containsKey(name);
    }

    public String getUrl(String name){
        return urls.get(name);
    }
}
