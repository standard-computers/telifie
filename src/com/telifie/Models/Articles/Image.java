package com.telifie.Models.Articles;

import org.bson.Document;

import java.util.UUID;

public class Image {

    private String id, url, caption, source;

    public Image(String url, String caption, String source) {
        this.id = UUID.randomUUID().toString();
        this.url = url;
        this.caption = caption;
        this.source = source;
    }

    public Image(Document document) throws NullPointerException {
        this.id = document.getString("id");
        this.url = document.getString("url");
        this.caption = document.getString("caption");
        this.source = document.getString("source");
    }

    @Override
    public String toString() {
        return "{" +
                (id == null || id.equals("null") ? "" : "\"id\" : \"" + id + "\",") +
                (url == null || url.equals("null") ? "" : "\"url\" : \"" + url + "\",") +
                (caption == null || caption.equals("null") ? "" : "\"caption\" : \"" + caption + "\",") +
                (source == null || source.equals("null") ? "" : "\"source\" : \"" + source + '\"') +
                '}';
    }

    public void setId(String id) {
        this.id = id;
    }
}
