package com.telifie.Models.Articles;

import org.bson.Document;

public class Image {

    private final String url;
    private final String caption;
    private final String source;

    public Image(String url, String caption, String source) {
        this.url = url;
        this.caption = caption;
        this.source = source;
    }

    public Image(Document document) {
        this.url = document.getString("url");
        this.caption = document.getString("caption");
        this.source = document.getString("source");
    }

    @Override
    public String toString() {
        return new StringBuilder().append("{")
            .append("\"url\" : \"").append(url).append("\", ")
            .append("\"caption\" : \"").append(caption).append("\", ")
            .append("\"source\" : \"").append(source).append("\"")
            .append("}").toString();
    }
}
