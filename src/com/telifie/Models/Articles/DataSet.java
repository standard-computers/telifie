package com.telifie.Models.Articles;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.bson.Document;
import java.util.ArrayList;
import java.util.UUID;

public class DataSet {

    private String id = UUID.randomUUID().toString(), title, source;
    private ArrayList<String[]> rows;

    public DataSet(Document document){
        this.id = (document.getString("id") == null ?  UUID.randomUUID().toString() : document.getString("id"));
        this.title = document.getString("title");
        this.source = document.getString("source");
        this.rows = (ArrayList<String[]>) document.get("rows", ArrayList.class);
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public ArrayList<String[]> getRows() {
        return rows;
    }

    public void setRows(ArrayList<String[]> rows) {
        this.rows = rows;
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
