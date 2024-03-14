package com.telifie.Models;

import org.bson.Document;
import java.util.ArrayList;

public class Association {

    private String name;
    private ArrayList<Child> articles = new ArrayList<>();

    public Association(Document document){
        this.name = document.getString("name");
        ArrayList<Document> a = (ArrayList<Document>) document.getList("articles", Document.class);
        a.forEach(doc -> this.articles.add(new Child(doc)));
    }

    @Override
    public String toString() {
        return new StringBuilder().append("{\"name\" : \"").append(name).append('\"').append(", \"articles\" : ").append(articles).append('}').toString();
    }

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
}