package com.telifie.Models.Articles;

import org.bson.Document;

public class Image {

    private String url, caption, source;

    public Image(String url, String caption, String source) {
        this.url = url;
        this.caption = caption;
        this.source = source;
    }

    public Image(Document document) throws NullPointerException {
        this.url = document.getString("url");
        this.caption = document.getString("caption");
        this.source = document.getString("source");
    }

    @Override
    public String toString() {
        return "{" +
                (url == null || url.equals("null") ? "" : "\"url\" : \"" + url + "\",") +
                (caption == null || caption.equals("null") ? "" : "\"caption\" : \"" + caption + "\",") +
                (source == null || source.equals("null") ? "" : "\"source\" : \"" + source + '\"') +
                '}';
    }
}
