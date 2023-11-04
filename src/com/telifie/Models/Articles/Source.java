package com.telifie.Models.Articles;

import org.bson.Document;

public class Source {

    private final String icon;
    private String name;
    private final String url;

    public Source(String icon, String name, String url) {
        this.icon = icon;
        this.name = name;
        this.url = url;
    }

    public Source(Document document) throws NullPointerException {
        this.icon = document.getString("icon");
        this.name = document.getString("name");
        this.url = document.getString("url");
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
        return new StringBuilder().append("{\"id\" : \"").append('\"').append(", \"icon\" : \"").append(icon).append('\"').append(", \"name\" : \"").append(name).append('\"').append(", \"url\" : \"").append(url).append("\"}").toString();
    }
}
