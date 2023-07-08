package com.telifie.Models;

import org.bson.Document;

public class Member {

    private final String name;
    private final String email;
    private final int permissions;

    public Member(Document doc){
        this.name = doc.getString("name");
        this.email = doc.getString("email");
        this.permissions = (doc.getInteger("permissions") == null ? 0 : doc.getInteger("permissions"));
    }

    @Override
    public String toString(){
        return "{" +
                "\"name\" : \"" + this.name + "\"" +
                ", \"email\" : \"" + this.email + "\"" +
                ", \"permissions\" : " + this.permissions +
                "}";
    }
}