package com.telifie.Models;

import com.telifie.Models.Utilities.Tool;
import org.bson.Document;
import org.json.JSONObject;
import java.io.Serializable;

public class Source implements Serializable {

    private String id, icon, name, url, description = "";

    public Source(Document document) throws NullPointerException {
        this.id = (document.getString("id") == null ? Tool.md5(Tool.eid()) : document.getString("id") );
        this.icon = document.getString("icon");
        this.name = document.getString("name");
        this.url = document.getString("url");
        this.description = document.getString("description");
    }

    public String getId() {
        return id;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public String toString() {
        return "{\"id\" : \"" + id + '\"' +
                ", \"icon\" : \"" + icon + '\"' +
                ", \"name\" : \"" + name + '\"' +
                ", \"url\" : \"" + url + '\"' +
                ", \"description\" : \"" + description + "\"}";
    }
}
