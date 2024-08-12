package com.telifie.Models.Utilities;

public class Service {

    private String id, name, type, description, access, secret, refresh;
    private int version, origin;

    public Service(String id, String name, String type, String description, String access, String secret, String refresh, int version, int origin) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.description = description;
        this.access = access;
        this.secret = secret;
        this.refresh = refresh;
        this.version = version;
        this.origin = origin;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return "{\"id\" : \"" + id + '\"' +
                ", \"name\" : \"" + name + '\"' +
                ", \"type\" : \"" + type + '\"' +
                ", \"description\" : \"" + description + "\"}";
    }
}