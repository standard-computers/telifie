package com.telifie.Models.Articles;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.bson.Document;
import java.util.ArrayList;
import java.util.UUID;

public class DataSet {

    private String id, title, source;
    private ArrayList<String[]> rows = new ArrayList<>();

    public DataSet(Document document){
        this.id = (document.getString("id") == null ?  UUID.randomUUID().toString() : document.getString("id"));
        this.title = document.getString("title");
        this.source = document.getString("source");
        this.rows = (ArrayList<String[]>) document.get("rows", ArrayList.class);
    }

    public DataSet(String title){
        this.title = title;
    }

    public void add(String[] row){
        this.rows.add(row);
    }

    @Override
    public String toString() {
        ObjectMapper objectMapper = new ObjectMapper();
        String json;
        try {
            json = objectMapper.writeValueAsString(rows);
        } catch (JsonProcessingException e) {
            json = rows.toString();
        }

        return "{\"id\" : \"" + id + "\"" +
                ", \"title\" : \"" + title + "\"" +
                (source == null ? "" : ", \"source\" : \"" + source + "\"" ) +
                ", \"rows\" : " + json +
                '}';
    }
}
