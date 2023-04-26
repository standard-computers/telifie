package com.telifie.Models.Articles;

public class Attribute {

    private String key, value;

    public Attribute(String key, String value) {
        this.key = key;
        this.value = value;
    }

    public String getKey() {
        return this.key;
    }

    @Override
    public String toString() {
        return "{\"key\" : \"" + key + "\", \"value\" : \"" + value + "\"}";
    }
}
