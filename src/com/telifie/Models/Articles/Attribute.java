package com.telifie.Models.Articles;

import com.telifie.Models.Utilities.Telifie;

public class Attribute {

    private String key, value;

    public Attribute(String key, String value) {
        this.key = Telifie.tools.strings.escape(key);
        this.value = Telifie.tools.strings.escape(value);
    }

    public String getKey() {
        return this.key;
    }

    @Override
    public String toString() {
        return "{\"key\" : \"" + key + "\", \"value\" : \"" + value + "\"}";
    }
}
