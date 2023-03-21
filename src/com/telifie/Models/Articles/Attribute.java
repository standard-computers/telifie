package com.telifie.Models.Articles;

import org.json.JSONObject;
import java.io.Serializable;

public class Attribute implements Serializable {

    private String key, value;

    public Attribute(String key, String value) {
        this.key = key;
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
