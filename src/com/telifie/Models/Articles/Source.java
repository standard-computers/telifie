package com.telifie.Models.Articles;

import org.bson.Document;
import java.util.UUID;

public class Source {

    private final String id;
    private final String icon;
    private String name;
    private final String url;

    public Source(String id, String icon, String name, String url) {
        this.id = id;
        this.icon = icon;
        this.name = name;
        this.url = url;
    }

    public Source(Document document) throws NullPointerException {
        this.id = (document.getString("id") == null ? UUID.randomUUID().toString() : document.getString("id") );
        this.icon = document.getString("icon");
        this.name = document.getString("name");
        this.url = document.getString("url");
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

    public String getUrl() {
        return url;
    }

    @Override
    public String toString() {
        return "{\"id\" : \"" + id + '\"' +
                ", \"icon\" : \"" + icon + '\"' +
                ", \"name\" : \"" + name + '\"' +
                ", \"url\" : \"" + url + "\"}";
    }
}
