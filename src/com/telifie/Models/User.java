package com.telifie.Models;

import org.bson.Document;
import java.io.Serializable;
import java.util.UUID;

public class User implements Serializable {

    private String id, email, name, phone, token, settings = "{\\\"secondary\\\":\\\"#0583c5\\\",\\\"corner_radius\\\":8,\\\"\\\":\\\"#1977F1\\\",\\\"color\\\":\\\"#0c92c2\\\",\\\"dark_mode\\\":\\\"0,1,2\\\",\\\"background\\\":\\\"#FFFFFF\\\",\\\"font_size\\\":16,\\\"name\\\":\\\"Facebook Blue\\\",\\\"foreground\\\":\\\"#000000\\\",\\\"border_color\\\":\\\"#BEBEBE\\\"}";
    private final int origin;
    private int permissions;

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
        this.phone = document.getString("phone");
        this.token = document.getString("token");
        this.origin = document.getInteger("origin");
        this.permissions = document.getInteger("permissions");
        this.settings = document.getString("settings");
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
                ", \"phone\" : \"" + phone + '\"' +
                ", \"origin\" : " + origin +
                ", \"permissions\" : " + permissions +
                ", \"settings\" : " + settings.replace("\\", "") +
                '}';
    }
}