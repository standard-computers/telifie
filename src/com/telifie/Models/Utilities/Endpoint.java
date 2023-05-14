package com.telifie.Models.Utilities;

import org.bson.Document;

public class Endpoint {

    private String url, method, description;

    public Endpoint(Document document){
        this.url = document.getString("url");
        this.method = document.getString("method");
        this.description = document.getString("description");
    }

    @Override
    public String toString() {

        return "{\"url\" : \"" + (url != null ? url : "") + '\"' +
                ", \"method\" : \"" + (method != null ? method :  "") + '\"' +
                ", \"description\" : \"" + (description != null ? description :  "") + '\"' +
                '}';
    }

}
