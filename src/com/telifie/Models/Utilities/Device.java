package com.telifie.Models.Utilities;

import com.telifie.Models.User;

public class Device {

    private String address, id, name, type;
    private User user;

    public Device(String address, String id, String name, String type, User user){
        this.address = address;
        this.id = id;
        this.name = name;
        this.type = type;
        this.user = user;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }
}
