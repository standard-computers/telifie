package com.telifie.Models;

import org.bson.Document;

import java.io.Serializable;

public class Domain implements Serializable {

    private String uri, id, alt, icon, owner, name;
    private int permissions;
    //TODO users

    public Domain(String uri){
        this.uri = uri;
    }

    public Domain(String name, String uri){
        this.name = name;
        this.uri = uri;
    }

    public Domain(Document document){
        this.id = document.getString("id");
        this.alt = document.getString("alt");
        this.icon = document.getString("icon");
        this.owner = document.getString("owner");
        this.name = document.getString("name");
        this.permissions = (document.getInteger("permissions") == null ? 0 : document.getInteger("permissions"));
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getAlt() {
        return alt;
    }

    public void setAlt(String alt) {
        this.alt = alt;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getPermissions() {
        return permissions;
    }

    public void setPermissions(int permissions) {
        this.permissions = permissions;
    }

    @Override
    public String toString() {
        return "{" +
                "\"uri\" : \"" + uri + '\"' +
                ", \"id\" : \"" + id + '\"' +
                ", \"alt\" : \"" + alt + '\"' +
                ", \"icon\" : \"" + icon + '\"' +
                ", \"owner\" : \"" + owner + '\"' +
                ", \"name\" : \"" + name + '\"' +
                ", \"permissions\" :" + permissions +
                '}';
    }
}
