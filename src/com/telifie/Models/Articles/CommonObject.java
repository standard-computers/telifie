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

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getOrigin() {
        return origin;
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
