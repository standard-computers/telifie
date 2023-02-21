package com.telifie.Models;

import com.telifie.Models.Utilities.Tool;
import org.bson.Document;

import java.io.Serializable;

public class Domain implements Serializable {

    private String uri, id, alt, icon, owner, name;
    private int origin, permissions;
    //TODO users

    public Domain(String uri){
        this.uri = uri;
    }

    public Domain(String name, String uri){
        this.name = name;
        this.uri = uri;
    }
    
    public Domain(String owner, String name, String icon, int permissions){
        this.owner = owner;
        this.id = Tool.md5(Tool.shortEid());
        this.alt = Tool.eid();
        this.name = name;
        this.icon = icon;
        this.origin = Tool.epochTime();
        this.permissions = (permissions <= 2 && permissions >= 0 ? permissions : 0); //Private is default mode if none
    }

    public Domain(Document document){
        this.id = (document.getString("id") == null ? Tool.md5(Tool.eid()) : document.getString("id"));
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

    public Domain setName(String name) {
        this.name = name;
        return this;
    }

    public int getOrigin() {
        return origin;
    }

    public int getPermissions() {
        return permissions;
    }

    public void setPermissions(int permissions) {
        this.permissions = permissions;
    }

    public void makePrivate(){
        this.permissions = 0;
    }

    public void makePublic(){
        this.permissions = 1;
    }

    public void makeProtected(){
        this.permissions = 2;
    }

    @Override
    public String toString() {
        return "{" +
                (uri == null ? "" : "\"uri\" : \"" + uri + "\", ") +
                "\"id\" : \"" + id + '\"' +
                ", \"alt\" : \"" + alt + '\"' +
                ", \"icon\" : \"" + icon + '\"' +
                ", \"owner\" : \"" + owner + '\"' +
                ", \"name\" : \"" + name + '\"' +
                ", \"origin\" :" + origin +
                ", \"permissions\" :" + permissions +
                '}';
    }
}
