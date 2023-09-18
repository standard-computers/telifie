package com.telifie.Models.Articles;

import org.bson.Document;
import java.util.UUID;

public class Child {

    private final String image;
    private final String title;
    private final String reference;

    public Child(String image, String title, String reference) {
        this.image = image;
        this.title = title;
        this.reference = reference;
    }

    public Child(Document document){
        this.image = document.getString("image");
        this.title = document.getString("title");
        this.reference = document.getString("reference");
    }

    @Override
    public String toString() {
        return new StringBuilder().append("{")
            .append("\"image\" : \"").append(image).append('\"')
            .append(", \"title\" : \"").append(title).append('\"')
            .append(", \"reference\" : \"").append(reference).append('\"')
            .append('}').toString();
    }
}