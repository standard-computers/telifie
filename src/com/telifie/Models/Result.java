package com.telifie.Models;

import com.telifie.Models.Utilities.CommonObject;
import java.io.Serializable;
import java.util.ArrayList;

public class Result implements Serializable {

    public static final String APPLICATION_JSON = "application/json", TEXT_PLAIN = "text/plain";

    private String type = APPLICATION_JSON, query = "", object = "results", results = "";
    private int statusCode = 200, count = 0;
    private ArrayList<CommonObject> quickResults = new ArrayList<>();

    public Result(String query){
        this.query = query;
    }

    public Result(String query, String object, String results, int count) {
        this.query = query;
        this.object = object;
        this.results = results;
        this.count = count;
    }

    public Result(int statusCode, String results) {
        this.type = APPLICATION_JSON;
        this.statusCode = statusCode;
        this.results = results;
    }

    public Result(int statusCode, String query, String results) {
        this.type = APPLICATION_JSON;
        this.statusCode = statusCode;
        this.query = query;
        this.results = results;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public Result setStatusCode(int statusCode) {
        this.statusCode = statusCode;
        return this;
    }

    public String getType() {
        return type;
    }

    public Result setType(String type) {
        this.type = type;
        return this;
    }

    public String getQuery() {
        return query;
    }

    public String getObject() {
        return object;
    }

    public Result setObject(String object) {
        this.object = object;
        return this;
    }

    public int getCount() {
        return count;
    }

    public Result setCount(int count) {
        this.count = count;
        return this;
    }

    public ArrayList<CommonObject> getQuickResults() {
        return quickResults;
    }

    public void addQuickResult(CommonObject obj){
        this.quickResults.add(obj);
    }

    public String getResults() {
        return results;
    }

    public Result setResults(String results) {
        this.results = results;
        return this;
    }

    @Override
    public String toString() {
        return "{\"status_code\" : " + statusCode +
                ", \"query\" : \"" + query + '\"' +
                ", \"count\" : " + count +
                (quickResults.size() > 0 && quickResults != null ? ", \"quick_results\" : " + quickResults : "") +
                ", \"" + object + "\" : " + results +
                '}';
    }
}
