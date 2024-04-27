package com.telifie.Models;

import com.telifie.Models.Utilities.Parameters;
import com.telifie.Models.Utilities.Telifie;
import org.bson.Document;
import java.util.ArrayList;
import java.util.UUID;

public class Result {

    private final String id = UUID.randomUUID().toString();
    public final String query;
    private String source = "";
    private Parameters params;
    private String object = "results";
    private String generated = "";
    private Object results;
    private final int origin = Telifie.epochTime();
    private int statusCode = 200, count = 0, total;

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

    public Result(int statusCode, String query, String results) {
        this.statusCode = statusCode;
        this.query = query;
        this.results = results;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public void setTotal(int total) {
        this.total = total;
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

    public void setParams(Parameters params) {
        this.params = params;
    }

    public void setResults(String object, Object results) {
        this.object = object;
        this.results = results;
    }

    public String getGenerated() {
        return this.generated;
    }
}