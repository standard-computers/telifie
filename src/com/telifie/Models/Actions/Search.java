package com.telifie.Models.Actions;

import com.mongodb.client.model.Filters;
import com.telifie.Models.Andromeda.Andromeda;
import com.telifie.Models.Andromeda.Unit;
import com.telifie.Models.Article;
import com.telifie.Models.Clients.ArticlesClient;
import com.telifie.Models.Connectors.Radar;
import com.telifie.Models.Connectors.Rest;
import com.telifie.Models.Result;
import com.telifie.Models.Utilities.Packages;
import com.telifie.Models.Utilities.Parameters;
import com.telifie.Models.Utilities.Session;
import org.bson.Document;
import org.bson.conversions.Bson;
import java.io.IOException;
import java.util.*;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Search {

    public Result execute(Session session, String q, Parameters params){
        ArticlesClient articles = new ArticlesClient(session);
        Unit query = new Unit(q);
        Result result = new Result(200, query.text(), "");
        boolean doquery = true;
        if(params.getPage() == 1){
            if(query.text().contains("*") || query.text().contains("+") || query.text().contains("-") || query.text().contains("/") || Andromeda.tools.contains(Andromeda.NUMERALS, query.text())){
                String mathExpressionPattern = "[\\d\\s()+\\-*/=xX^sincosTanSECcscCot]+";
                Pattern pattern = Pattern.compile(mathExpressionPattern);
                Matcher matcher = pattern.matcher(query.text());
                while (matcher.find()) {
                    String match = matcher.group().trim();
                    if(Packages.get("com.telifie.connectors.wolfram") != null){
                        doquery = false;
                        result.setSource("com.telifie.connectors.wolfram");
                        result.setGenerated(Rest.get(Packages.get("com.telifie.connectors.wolfram"), new HashMap<>() {{
                            put("appid", Packages.get("com.telifie.connectors.wolfram").getAccess());
                            put("i", match);
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
//else if(query.startsWith(Andromeda.taxon("interrogative"))){}else if(query.startsWith(Andromeda.taxon("verb"))){
        }
        if(doquery){
            Document filter = filter(query, params);
            Bson sf = Filters.ne("description", "Image");
            if(params.getIndex().equals("images")){
                sf = Filters.eq("description", "Image");
            }else if(params.getIndex().equals("locations")){
                sf = Filters.exists("location");
            }
            filter = new Document("$and", Arrays.asList(sf, Filters.or(filter)));
            ArrayList<Article> results = articles.search(query, params, filter);
            ArrayList<Article> paged = paginate(results, params.getPage(), params.getRpp());
            result.setParams(params);
            result.setResults("articles", paged);
            result.setTotal(results.size());
        }
        return result;
    }

    private Document filter(Unit q, Parameters params){

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
        List<Document> or = new ArrayList<>();
        for (String word : q.tokens()) {
            or.add(new Document("title", pattern(word)));
        }
        or.add(new Document("tags", new Document("$in", Collections.singletonList(q.text()))));
        return new Document("$or", or);
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