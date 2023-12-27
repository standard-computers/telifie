package com.telifie.Models.Articles;

import org.bson.Document;

public class Child {

    private final String id, image, title, reference;

    public Child(Document document){
        this.id = document.getString("id");
        this.image = document.getString("image");
        this.title = document.getString("title");
        this.reference = document.getString("reference");
    }

    @Override
    public String toString() {
        return new StringBuilder().append("{\"id\" : \" ").append(id).append("\", \"image\" : \"").append(image).append("\", \"title\" : \"").append(title).append("\", \"reference\" : \"").append(reference).append("\"}").toString();
    }
}