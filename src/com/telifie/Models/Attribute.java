package com.telifie.Models;

import com.telifie.Models.Utilities.Telifie;

public record Attribute(String key, String value) {

    public Attribute(String key, String value) {
        this.key = Telifie.tools.htmlEscape(key);
        this.value = Telifie.tools.escape(value);
    }

    @Override
    public String toString() {
        return new StringBuilder().append("{\"key\" : \"").append(key).append("\", \"value\" : \"").append(value).append("\"}").toString();
    }
}