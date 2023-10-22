package com.telifie.Models;

import com.telifie.Models.Utilities.Parameters;
import com.telifie.Models.Utilities.Telifie;
import org.bson.Document;
import java.util.ArrayList;
import java.util.UUID;

public class Result {

    private final String id = UUID.randomUUID().toString();
    private String query = "", source = "";
    private Parameters params;
    private String object = "results";
    private String generated = "";
    private Object results;
    private final int origin = Telifie.epochTime();
    private int statusCode = 200, count = 0, total;
    private ArrayList<Article> quickResults = new ArrayList<>();

    public Result(String query, String object, ArrayList results) {
        this.query = query;
        this.object = object;
        this.results = results;
        this.count = results.size();
    }

    public Result(String query, Parameters params, String object, ArrayList results) {
        this.query = query;
        this.params = params;
        this.object = object;
        this.results = results;
        this.count = results.size();
    }

    public Result(String query, String object, Object results) {
        this.query = query;
        this.object = object;
        this.results = results;
    }

    public Result(int statusCode, String results) {
        this.statusCode = statusCode;
        this.results = results;
    }

    public Result(int statusCode, String query, String results) {
        this.statusCode = statusCode;
        this.query = query;
        this.results = results;
    }

    public Result(String query, Parameters params, ArrayList<Article> quickResults, ArrayList<Article> results) {
        this.query = query;
        this.object = "articles";
        this.results = results;
        this.quickResults = quickResults;
        this.count = results.size();
    }

    public int getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }

    public void setTotal(int total) {
        this.total = total;
    }

    public void setResult(String object, Object results){
        this.object = object;
        this.results = results;
    }

    public void setGenerated(String generated) {
        this.generated = generated;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("{").append("\"status_code\" : ").append(statusCode).append(", \"id\" : \"").append(id).append('\"').append(", \"query\" : \"").append(query).append('\"');
        if (!source.isEmpty()) {
            sb.append(", \"source\" : \"").append(source).append('\"');
        }
        if (!generated.isEmpty()) {
            sb.append(", \"generated\" : \"").append(generated).append('\"');
        }
        sb.append(", \"count\" : ").append(count).append(", \"total\" : ").append(total).append(", \"origin\" : ").append(origin);
        if (!quickResults.isEmpty()) {
            sb.append(", \"quick_results\" : ").append(quickResults);
        }
        sb.append(", \"").append(object).append("\" : ");
        if (results instanceof String) {
            sb.append("\"").append(results).append("\"");
        } else if (results instanceof Document) {
            sb.append(((Document) results).toJson());
        } else {
            sb.append(results);
        }
        sb.append("}");
        return sb.toString();
    }

}
