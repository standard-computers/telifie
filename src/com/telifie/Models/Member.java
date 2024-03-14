package com.telifie.Models;

import org.bson.Document;

public class Member {

    private final String id;
    private final int permissions;

    public Member(Document doc){
        this.id = doc.getString("id");
        this.permissions = (doc.getInteger("permissions") == null ? 0 : doc.getInteger("permissions"));
    }

    public String getId() {
        return id;
    }

    public int permissions() {
        return permissions;
    }

    @Override
    public String toString(){
        return new StringBuilder().append("{\"id\" : \"").append(id).append("\", \"permissions\" : \"").append(permissions).append("\"}").toString();
    }
}