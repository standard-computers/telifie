package com.telifie.Models.Articles;

import org.bson.Document;

public class Child {

    private String id;
    private final String image;
    private final String title;
    private final String reference;

    public Child(String id, String image, String title, String reference) {
        this.id = id;
        this.image = image;
        this.title = title;
        this.reference = reference;
    }

    public Child(Document document){
        this.id = document.getString("id");
        this.image = document.getString("image");
        this.title = document.getString("title");
        this.reference = document.getString("reference");
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Override
    public String toString() {
        return "{" +
                "\"id\" : \"" + id + '\"' +
                (image == null || image.equals("null") ? "" : ", \"image\" : \"" + image + '\"') +
                (title == null || title.equals("null") ? "" : ", \"title\" : \"" + title + '\"') +
                (reference == null || reference.equals("null") ? "" : ", \"reference\" : \"" + reference + '\"') +
                '}';
    }
}
