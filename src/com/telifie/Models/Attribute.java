package com.telifie.Models;

import org.json.JSONObject;
import java.io.Serializable;

public class Attribute implements Serializable {

    private String key, value;

    public Attribute(String key, String value) {
        this.key = key;
        this.value = value;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return "{" +
                "\"key\" : \"" + key + '\"' +
                ", \"value\" : \"" + value + '\"' +
                '}';
    }

    public JSONObject toJson(){
        JSONObject json = new JSONObject(this.toString());
        return json;
    }

}
