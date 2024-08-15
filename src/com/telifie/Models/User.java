package com.telifie.Models;

import com.telifie.Models.Utilities.Telifie;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

public class User {

    private final String id, email, name, phone;
    private final int origin;
    private int permissions;

    public User(String email, String name, String phone) {
        this.id = UUID.randomUUID().toString();
        this.email = email;
        this.name = name;
        this.phone = phone;
        this.origin = Telifie.time();
        this.permissions = 0;
    }

    public User(ResultSet result) throws SQLException {
        this.id = result.getString("id");
        this.email = result.getString("email");
        this.name = result.getString("name");
        this.phone = result.getString("phone");
        this.origin = result.getInt("origin");
        this.permissions = result.getInt("permissions");
    }

    public String getId() {
        return id;
    }

    @Override
    public String toString() {
        return "{\"id\" : \"" + id + '\"' + ", \"email\" : \"" + email + '\"' + ", \"name\" : \"" + name + '\"' + ", \"phone\" : \"" + phone + '\"' + ", \"origin\" : " + origin + ", \"permissions\" : " + permissions + '}';
    }
}