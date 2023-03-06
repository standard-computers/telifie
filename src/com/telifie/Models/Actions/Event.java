package com.telifie.Models.Actions;

import org.bson.Document;
import org.json.JSONObject;

public class Event {

    private Type type;
    private int origin;
    private String user = "GUEST", content;

    public enum Type {
        UPDATE, POST, GET, PUT, SEARCH, MESSAGE, EMAIL, TEXT, FLAG, DELETE
    }

    public Event(Type type, int origin, String user, String content) {
        this.type = type;
        this.origin = origin;
        this.user = user;
        this.content = content;
    }

    public Event(Document document){
        this.type = Type.valueOf(document.getString("type"));
        this.origin = document.getInteger("origin");
        this.user = document.getString("user");
        this.content = document.getString("content");
    }

    public Type getType() {
        return type;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public int getOrigin() {
        return origin;
    }

    public String getContent() {
        return content;
    }

    @Override
    public String toString() {
        return "{\"type\" : \"" + type + '\"' +
                ", \"user\" : \"" + user + '\"' +
                ", \"origin\" : " + origin +
                ", \"content\" : \"" + content + '\"' +
                '}';
    }

    public JSONObject toJson(){
        return new JSONObject(this.toString());
    }

}
