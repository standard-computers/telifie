package com.telifie.Models.Utilities;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class Service {

    private String id, name, type, description, access, secret, refresh;
    private int version, origin;

    @JsonCreator
    public Service(@JsonProperty("id") String id, @JsonProperty("name") String name, @JsonProperty("type") String type, @JsonProperty("description") String description, @JsonProperty("access") String access, @JsonProperty("secret") String secret, @JsonProperty("refresh") String refresh, @JsonProperty("version") int version, @JsonProperty("origin") int origin) {
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