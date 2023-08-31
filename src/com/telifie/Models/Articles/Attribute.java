package com.telifie.Models.Articles;

import com.telifie.Models.Utilities.Telifie;

public record Attribute(String key, String value) {

    public Attribute(String key, String value) {
        this.key = Telifie.tools.strings.escape(key);
        this.value = Telifie.tools.strings.escape(value);
    }

    @Override
    public String toString() {
        return new StringBuilder().append("{").append("\"key\" : \"").append(key).append("\", ").append("\"value\" : \"").append(value).append("\"").append("}").toString();
    }
}
