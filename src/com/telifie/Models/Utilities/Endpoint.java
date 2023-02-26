package com.telifie.Models.Utilities;

import org.bson.Document;

public class Endpoint {

    private String url, method, description;

    public Endpoint(Document document){
        this.url = document.getString("url");
        this.method = document.getString("method");
        this.description = document.getString("description");
    }

    public Endpoint(String url, String method, String description) {
        this.url = url;
        this.method = method;
        this.description = description;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public String toString() {

        return "{\"url\" : \"" + (url != null ? url : "") + '\"' +
                ", \"method\" : \"" + (method != null ? method :  "") + '\"' +
                ", \"description\" : \"" + (description != null ? description :  "") + '\"' +
                '}';
    }

}
