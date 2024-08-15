package com.telifie.Models.Utilities;

import org.bson.Document;

public class Event {

    private final Type type;
    private final int origin;
    private final String user;
    private final String content;

    public enum Type {
        PUT, MESSAGE, FLAG, ERROR
    }

    public Event(Document document){
        this.type = Type.valueOf(document.getString("type"));
        this.origin = document.getInteger("origin");
        this.user = document.getString("user");
        this.content = document.getString("content");
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"type\" : \"").append(type).append('\"').append(", \"user\" : \"").append(user).append('\"').append(", \"origin\" : ").append(origin).append(", \"content\" : \"").append(content).append('\"').append('}');
        return sb.toString();
    }
}