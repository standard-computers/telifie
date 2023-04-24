package com.telifie.Models;

import com.telifie.Models.Articles.CommonObject;
import java.util.ArrayList;
import java.util.UUID;

public class Result {

    private String id = UUID.randomUUID().toString(), query = "", object = "results";
    private Object results;
    private int statusCode = 200, count = 0;
    private ArrayList<CommonObject> quickResults = new ArrayList<>();

    public Result(String query, String object, ArrayList results) {
        this.query = query;
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

    public Result(String query, ArrayList<CommonObject> quickResults, ArrayList<Article> results) {
        this.query = query;
        this.object = "articles";
        this.results = results;
        this.quickResults = quickResults;
        this.count = results.size();
    }

    public int getStatusCode() {
        return statusCode;
    }

    public Result setStatusCode(int statusCode) {
        this.statusCode = statusCode;
        return this;
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
                ", \"count\" : " + count +
                (quickResults.size() > 0 && quickResults != null ? ", \"quick_results\" : " + quickResults : "") +
                ", \"" + object + "\" : " + results.toString() +
                "}";
    }
}
