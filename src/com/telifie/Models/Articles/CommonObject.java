package com.telifie.Models.Articles;

import org.bson.Document;

public class CommonObject {

    private String id, icon, title, link, description;
    private int origin;

    public CommonObject(String icon, String title, String link, String description) {
        this.icon = icon;
        this.title = title;
        this.link = link;
        this.description = description;
        this.origin = (int) (System.currentTimeMillis() / 1000);
    }

    public CommonObject(Document document){
        this.icon = document.getString("icon");
        this.title = document.getString("title");
        this.link = document.getString("link");
        this.description = document.getString("description");
        //TODO \"incorporate\" ORIGIN\"
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
                ", \"icon\" : \"" + icon + '\"' +
                ", \"title\" : \"" + title + '\"' +
                ", \"link\" : \"" + link + '\"' +
                ", \"description\" : \"" + description + '\"' +
                ", \"origin\" : " + origin +
                '}';
    }
}
