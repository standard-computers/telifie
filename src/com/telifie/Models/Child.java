package com.telifie.Models;

import org.bson.Document;

class Child {

    private String id, image, title, reference;

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

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }

    @Override
    public String toString() {
        return "{" +
                "\"id\" : \"" + id + '\"' +
                ", \"image\" : \"" + image + '\"' +
                ", \"title\" : \"" + title + '\"' +
                ", \"reference\" : \"" + reference + '\"' +
                '}';
    }
}
