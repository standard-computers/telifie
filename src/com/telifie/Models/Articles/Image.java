package com.telifie.Models.Articles;

import org.bson.Document;
import org.json.JSONObject;

import java.util.UUID;

public class Image {

    private String id;
    private final String url;
    private final String caption;
    private final String source;

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
        JSONObject jsonObj = new JSONObject();
        if (id != null && !id.isEmpty()) jsonObj.put("id", id);
        if (url != null && !url.isEmpty()) jsonObj.put("url", url);
        if (caption != null && !caption.isEmpty()) jsonObj.put("caption", caption);
        if (source != null && !source.isEmpty()) jsonObj.put("source", source);
        return jsonObj.toString();
    }

    public void setId(String id) {
        this.id = id;
    }
}
