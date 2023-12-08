package com.telifie.Models.Articles;

import com.telifie.Models.Andromeda.Andromeda;

public record Attribute(String key, String value) {

    public Attribute(String key, String value) {
        this.key = Andromeda.tools.htmlEscape(key);
        this.value = Andromeda.tools.htmlEscape(value);
    }

    @Override
    public String toString() {
        return new StringBuilder().append("{").append("\"key\" : \"").append(key).append("\", ").append("\"value\" : \"").append(value).append("\"").append("}").toString();
    }
}
