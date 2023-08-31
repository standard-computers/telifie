package com.telifie.Models.Utilities;

import org.bson.Document;

public class Event {

    private final Type type;
    private final int origin;
    private String user;
    private final String content;

    public enum Type {
        UPDATE, POST, GET, PUT, SEARCH, MESSAGE, EMAIL, TEXT, FLAG, DELETE, CRAWL
    }

    public Event(Type type, int origin, String user, String content) {
        this.type = type;
        this.origin = origin;
        this.user = user;
        this.content = content;
    }

    public Event(Type type, String user, String content) {
        this.type = type;
        this.origin = (int) (System.currentTimeMillis() / 1000);
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

    public int getOrigin() {
        return origin;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"type\" : \"").append(type).append('\"')
                .append(", \"user\" : \"").append(user).append('\"')
                .append(", \"origin\" : ").append(origin)
                .append(", \"content\" : \"").append(content).append('\"')
                .append('}');
        return sb.toString();
    }
}
