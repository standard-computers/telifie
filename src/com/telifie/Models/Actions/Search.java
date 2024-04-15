package com.telifie.Models.Actions;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.telifie.Models.Andromeda.Andromeda;
import com.telifie.Models.Andromeda.Unit;
import com.telifie.Models.Article;
import com.telifie.Models.Clients.ArticlesClient;
import com.telifie.Models.Clients.Cache;
import com.telifie.Models.Connectors.Radar;
import com.telifie.Models.Utilities.Network.Rest;
import com.telifie.Models.Result;
import com.telifie.Models.Clients.Packages;
import com.telifie.Models.Utilities.Parameters;
import com.telifie.Models.Utilities.Session;
import org.bson.Document;
import org.json.JSONArray;
import java.io.IOException;
import java.util.*;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Search {

    public Search() {
    }

    public Result execute(Session session, String q, Parameters params) throws JsonProcessingException {
        ArticlesClient articles = new ArticlesClient(session);
        Unit query = new Unit(q);
        Result result = new Result(200, query.text(), "");
        result.setParams(params);
        boolean doQuery = true;
        if(params.page == 1){
            if((query.text().contains("*") || query.text().contains("+") || query.text().contains("-") || query.text().contains("/")) && Andromeda.tools.contains(Andromeda.NUMERALS, query.text())){
                String mathExpressionPattern = "[\\d\\s()+\\-*/=xX^sincoaet]+";
                Pattern pattern = Pattern.compile(mathExpressionPattern);
                Matcher matcher = pattern.matcher(query.text());
                if(matcher.find()) {
                    if(Packages.get("com.telifie.connectors.wolfram") != null){
                        doQuery = false;
                        result.setSource("com.telifie.connectors.wolfram");
                        result.setGenerated(Rest.get(Packages.get("com.telifie.connectors.wolfram"), new HashMap<>() {{
                            put("appid", Packages.get("com.telifie.connectors.wolfram").getAccess());
                            put("i", query.text());
                        }}));
                    }
                }
            }else if(query.text().contains("uuid")){

                result.setGenerated("Here's a UUID  \\n" + UUID.randomUUID());
            }else if(query.text().contains("weather")){

                if(Packages.get("com.telifie.connectors.openweathermap") != null){
                    result.setSource("com.telifie.connectors.openweathermap");
                    result.setGenerated(Andromeda.tools.escape(Rest.get(Packages.get("com.telifie.connectors.openweathermap"), new HashMap<>() {{
                        put("units", "imperial");
                        put("excluded", "hourly,minutely,current");
                        put("lat", String.valueOf(params.getLatitude()));
                        put("lon", String.valueOf(params.getLongitude()));
                        put("appid", Packages.get("com.telifie.connectors.openweathermap").getAccess());
                    }})));
                }
            }else if(query.text().contains("flip a coin")){

                result.setGenerated(((new Random().nextInt(2) == 0) ? "Heads" : "Tails"));
            }else if(query.text().contains("roll") && query.text().contains("dice")){

                int random = new Random().nextInt(6) + 1;
                result.setGenerated("Your dice roll is " + random);
            }else if(query.containsAddress()){

                try {
                    Radar.get(query.text());
                } catch (IOException | InterruptedException e) {
                    throw new RuntimeException(e);
                }
                //TODO map/radar lookup
            }
        }
        if(doQuery && !params.quickResults){
            ArrayList<Article> results;
            String fromCache = Cache.get(query.cleaned());
            if(fromCache != null){
                results = new ObjectMapper().readValue(fromCache, ArrayList.class);
                result.setResults("articles", new JSONArray(results));
            }else{
                ArrayList<Article> all = articles.search(query, params, filter(query, params));
                results = paginate(all, params.page, params.rpp);
                CompletableFuture.runAsync(() -> Cache.cache(session.user, q, results.toString(), params));
                result.setResults("articles", results);
            }
            result.setTotal(results.size());
        }
        return result;
    }

    public static Document filter(Unit q, Parameters params){

        if(q.text().startsWith("@")){

            String p = q.text().split(" ")[0].replace("@","");
            String spl = q.text().replace("@" + p, "");
            return new Document(p, Pattern.compile("\\b" + Pattern.quote(spl.trim()) + "\\b", Pattern.CASE_INSENSITIVE));
        }else if(q.contains(Andromeda.taxon("proximity"))){
            String splr = q.get(Andromeda.taxon("proximity"));
            params.setIndex("locations");
            String[] spl = q.text().split(splr);
            if(spl.length >= 2){
                String subject = spl[0].trim();
                String place = spl[1].trim();
                Article pl = new ArticlesClient(new Session("", "telifie")).findPlace(place, params);
                params.setLatitude( Double.parseDouble(pl.getAttribute("Longitude")));
                params.setLongitude(Double.parseDouble(pl.getAttribute("Latitude")));
                return new Document("$and", Arrays.asList(
                    new Document("$or", Arrays.asList(
                            new Document("tags", pattern(subject)),
                            new Document("description", pattern(subject)),
                            new Document("title", pattern(subject))
                    )), new Document("location", new Document("$near", new Document("$geometry", new Document("type", "Point").append("coordinates", Arrays.asList(Double.parseDouble(pl.getAttribute("Longitude")), Double.parseDouble(pl.getAttribute("Latitude")))) ).append("$maxDistance", 16000)))
                ));
            }
        }
        if(q.tokens().length > 2){
            List<Document> or = new ArrayList<>();
            for (String word : q.tokens()) {
                or.add(new Document("title", pattern(word)));
            }
            or.add(new Document("tags", new Document("$in", Collections.singletonList(q.text()))));
            return new Document("$or", or);
        }
        return new Document("title", Pattern.compile("^" + Pattern.quote(q.text()), Pattern.CASE_INSENSITIVE));
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

    public static Pattern pattern(String value){
        return Pattern.compile("\\b" + Pattern.quote(value) + "\\b", Pattern.CASE_INSENSITIVE);
    }
}