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
    private final int origin = Telifie.getEpochTime();
    private int statusCode = 200, count = 0;
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

    public void setResult(String object, Object results){
        this.object = object;
        this.results = results;
    }

    @Override
    public String toString() {
        return "{\"status_code\" : " + statusCode +
                ", \"id\" : \"" + id + '\"' +
                ", \"query\" : \"" + query + '\"' +
                (params == null ? "" : ", \"params\" : " + params) +
                (source.isEmpty() ? "" : ", \"source\" : \"" + source + '\"') +
                (generated.isEmpty() ? "" : ", \"generated\" : \"" + generated + '\"') +
                ", \"count\" : " + count +
                ", \"origin\" : " + origin +
                (!quickResults.isEmpty() ? ", \"quick_results\" : " + quickResults : "") +
                ", \"" + object + "\" : " + (results instanceof String ? "\"" + results + "\"" : (results instanceof Document ? ((Document) results).toJson() : results.toString())) +
                "}";
    }
}
