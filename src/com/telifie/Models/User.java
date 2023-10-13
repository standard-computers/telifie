package com.telifie.Models;

import com.telifie.Models.Utilities.*;
import org.bson.Document;
import java.io.Serializable;
import java.util.UUID;

public class User implements Serializable {

    private String id;
    private final String email;
    private String name;
    private String photo = "";
    private String phone;
    private String token;
    private final int origin;
    private int permissions;
    private Theme theme;

    public User(String email) {
        this.email = email;
        this.origin = Telifie.epochTime();
        this.permissions = 0;
    }

    /**
     * Constructor for creating user.
     * Other data is autofill such as origin, permissions, and customerId
     * @param email User's email
     * @param name User's name
     * @param phone User's phone that receives texts
     */
    public User(String email, String name, String phone) {
        this.id = UUID.randomUUID().toString();
        this.email = email;
        this.name = name;
        this.phone = phone;
        this.origin = (int) (System.currentTimeMillis() / 1000);
        this.permissions = 0;
    }

    public User(Document document){
        this.id = document.getString("id");
        this.email = document.getString("email");
        this.name = document.getString("name");
        this.photo = document.getString("photo");
        this.phone = document.getString("phone");
        this.token = document.getString("token");
        this.origin = document.getInteger("origin");
        this.permissions = document.getInteger("permissions");
        this.theme = ( document.get("theme", Document.class) == null ? null : new Theme(document.get("theme", Document.class)) );
    }

    public String getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhone() {
        return phone;
    }

    public int getPermissions() {
        return permissions;
    }

    public void setPermissions(int permissions) {
        this.permissions = permissions;
    }

    public boolean hasToken(String attempt){
        return attempt.equals(this.token);
    }

    @Override
    public String toString() {
        return "{" +
                "\"id\" : \"" + id + '\"' +
                ", \"email\" : \"" + email + '\"' +
                ", \"name\" : \"" + name + '\"' +
                ", \"photo\" : \"" + photo + '\"' +
                ", \"phone\" : \"" + phone + '\"' +
                ", \"origin\" : " + origin +
                ", \"permissions\" : " + permissions +
                ", \"theme\" : " + theme +
                '}';
    }
}