package com.telifie.Models.Articles;

import org.bson.Document;
import java.util.ArrayList;

public class Association {

    private String icon, name;
    private ArrayList<Child> articles = new ArrayList<>();

    public Association(String icon, String name) {
        this.icon = icon;
        this.name = name;
    }

    public Association(Document document){
        this.icon = document.getString("icon");
        this.name = document.getString("name");

        ArrayList<Document> a = (ArrayList<Document>) document.getList("articles", Document.class);
        for(Document doc : a){
            this.addArticle(new Child(doc));
        }
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ArrayList<Child> getArticles() {
        return articles;
    }

    public void setArticles(ArrayList<Child> articles) {
        this.articles = articles;
    }

    public void addArticle(Child article){
        this.articles.add(article);
    }

    public int size(){
        return this.articles.size();
    }

    @Override
    public String toString() {
        return "{\"icon\" : \"" + icon + '\"' +
                ", \"name\" : \"" + name + '\"' +
                ", \"articles\" : " + articles +
                '}';
    }
}
