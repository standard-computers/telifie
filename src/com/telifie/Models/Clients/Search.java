package com.telifie.Models.Clients;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.telifie.Models.Article;
import com.telifie.Models.Utilities.*;
import com.telifie.Models.Result;
import org.bson.Document;
import java.util.*;
import java.util.regex.Pattern;

public class Search {

    public Result execute(Session session, String q, Parameters params) throws JsonProcessingException {
        Articles articles = new Articles(session, params.index);
        Result result = new Result(200, q, "");
        ArrayList<Article> results;
        ArrayList<Article> all = articles.search(q, params, filter(q, params));
        results = paginate(all, params.page, params.rpp);
        result.setResults("articles", results);
        result.setTotal(all.size());
        return result;
    }

    public static Document filter(String q, Parameters params){
        if(q.startsWith("@")){
            String p = q.split(" ")[0].replace("@","");
            String spl = q.replace("@" + p, "");
            return new Document(p, Pattern.compile("\\b" + Pattern.quote(spl.trim()) + "\\b", Pattern.CASE_INSENSITIVE));
        }
//      Article pl = new ArticlesClient(new Session("", "telifie")).findPlace(place, params);
//      params.setLatitude(Double.parseDouble(pl.getAttribute("Longitude")));
//      params.setLongitude(Double.parseDouble(pl.getAttribute("Latitude")));
//      return new Document("$and", Arrays.asList(
//          new Document("$or", Arrays.asList(
//                  new Document("description", pattern(subject)),
//                  new Document("title", pattern(subject))
//          )), new Document("location", new Document("$near", new Document("$geometry", new Document("type", "Point").append("coordinates", Arrays.asList(Double.parseDouble(pl.getAttribute("Longitude")), Double.parseDouble(pl.getAttribute("Latitude")))) ).append("$maxDistance", 16000)))
//      ));
        if(q.split(" ").length > 1){
            List<Document> or = new ArrayList<>();
            for (String word : q.split(" ")) {
                or.add(new Document("title", Pattern.compile("\\b" + Pattern.quote(word) + "\\b", Pattern.CASE_INSENSITIVE)));
            }
            return new Document("$or", or);
        }
        return new Document("title", Pattern.compile("\\b" + Pattern.quote(q) + "\\b", Pattern.CASE_INSENSITIVE));
    }

    private ArrayList<Article> paginate(ArrayList<Article> results, int page, int pageSize) {
        if(results.size() < pageSize){
            return results;
        }
        ArrayList<Article> paginatedResults = new ArrayList<>();
        int startIndex = (page - 1) * pageSize;
        int endIndex = Math.min(startIndex + pageSize, results.size());
        if (startIndex < results.size()) {
            paginatedResults.addAll(results.subList(startIndex, endIndex));
        }
        return paginatedResults;
    }
}