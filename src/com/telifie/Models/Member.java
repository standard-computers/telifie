package com.telifie.Models;

import org.bson.Document;

public class Member {

    private final String email;
    private final int permissions;

    public Member(Document doc){
        this.email = doc.getString("email");
        this.permissions = (doc.getInteger("permissions") == null ? 0 : doc.getInteger("permissions"));
    }

    public String getEmail() {
        return email;
    }

    public int permissions() {
        return permissions;
    }

    @Override
    public String toString(){
        return new StringBuilder().append("{\"email\" : \"").append(email).append("\", \"permissions\" : \"").append(permissions).append("\"}").toString();
    }
}