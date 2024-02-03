package com.telifie.Models.Articles;

import org.bson.Document;
import java.util.ArrayList;

public class DataSet {

    private final String title;
    private final ArrayList<String[]> rows;

    public DataSet(Document document){
        this.title = document.getString("title");
        this.rows = (ArrayList<String[]>) document.get("rows", ArrayList.class);
    }

    @Override
    public String toString() {
        StringBuilder jsonBuilder = new StringBuilder();
        jsonBuilder.append("{ \"title\": \"").append(title).append("\", \"rows\": [");
        for (String[] row : rows) {
            jsonBuilder.append("[\"").append(String.join("\", \"", row)).append("\"],");
        }
        if (!rows.isEmpty()) {
            jsonBuilder.deleteCharAt(jsonBuilder.length() - 1); // Remove the trailing comma
        }
        jsonBuilder.append("] }");
        return jsonBuilder.toString();
    }
}