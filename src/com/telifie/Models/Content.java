package com.telifie.Models;

import java.io.Serializable;

public class Content implements Serializable {

    private String title, body;

    public Content(String body) {
        this.body = body;
    }

    public Content(String title, String content) {
        this.title = title;
        this.body = content;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    @Override
    public String toString() {
        return "{" +
                (title == null ? "" : "title : '" + title + "\', ") +
                "body : '" + body + '\'' +
                '}';
    }
}
