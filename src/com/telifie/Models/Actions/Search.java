package com.telifie.Models.Actions;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.telifie.Models.Article;
import com.telifie.Models.Clients.ArticlesClient;
import com.telifie.Models.Utilities.*;
import com.telifie.Models.Utilities.Network.Rest;
import com.telifie.Models.Result;
import com.telifie.Models.Clients.Packages;
import org.bson.Document;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Search {

    public Search() {
    }

    public Result execute(Session session, String q, Parameters params) throws JsonProcessingException {
        ArticlesClient articles = new ArticlesClient(session);
        Result result = new Result(200, q, "");
        result.setParams(params);
        boolean doQuery = true;
        if(params.page == 1){
            Voyager.Unit unit = new Voyager.Unit(q);
            if((q.contains("*") || q.contains("+") || q.contains("-") || q.contains("/")) || Telifie.tools.contains(Telifie.NUMERALS, q)){
                String mathExpressionPattern = "[\\d\\s()+\\-*/=xX^sincoaet]+";
                Pattern pattern = Pattern.compile(mathExpressionPattern);
                Matcher matcher = pattern.matcher(q);
                if(matcher.find()) {
                    if(Packages.get("com.telifie.connectors.wolfram") != null){
                        doQuery = false;
                        result.setSource("com.telifie.connectors.wolfram");
                        result.setGenerated(Rest.get(Packages.get("com.telifie.connectors.wolfram"), new HashMap<>() {{
                            put("appid", Packages.get("com.telifie.connectors.wolfram").getAccess());
                            put("i", q);
                        }}));
                    }
                }
            }else if(q.contains("uuid")){

                result.setGenerated("Here's a UUID  \\n" + UUID.randomUUID());
            }else if(q.contains("weather")){

                if(Packages.get("com.telifie.connectors.openweathermap") != null){
                    result.setSource("com.telifie.connectors.openweathermap");
                    result.setGenerated(Telifie.tools.escape(Rest.get(Packages.get("com.telifie.connectors.openweathermap"), new HashMap<>() {{
                        put("units", "imperial");
                        put("excluded", "hourly,minutely,current");
                        put("lat", String.valueOf(params.latitude));
                        put("lon", String.valueOf(params.longitude));
                        put("appid", Packages.get("com.telifie.connectors.openweathermap").getAccess());
                    }})));
                }
            }else if(q.contains("flip a coin")){

                result.setGenerated(((new Random().nextInt(2) == 0) ? "Heads" : "Tails"));
            }else if(q.contains("roll") && q.contains("dice")){

                int random = new Random().nextInt(6) + 1;
                result.setGenerated("Your dice roll is " + random);
            }else if(Telifie.tools.containsAddress(q)){
                //TODO map/radar lookup
            }else if(unit.isInterrogative()){
                Log.console(unit.getSubject());
            }
        }
        if(doQuery){ //Actually execute against collections/domains
            ArrayList<Article> results;
            ArrayList<Article> all = articles.search(q, params, filter(q, params));
            results = paginate(all, params.page, params.rpp);
            result.setResults("articles", results);
            result.setTotal(results.size());
        }
        return result;
    }

    public static Document filter(String q, Parameters params){

        if(q.startsWith("@")){

            String p = q.split(" ")[0].replace("@","");
            String spl = q.replace("@" + p, "");
            return new Document(p, Pattern.compile("\\b" + Pattern.quote(spl.trim()) + "\\b", Pattern.CASE_INSENSITIVE));
        }
//        else if(q.contains(Andromeda.taxon("proximity"))){
//            String splr = q.get(Andromeda.taxon("proximity"));
//            params.setIndex("locations");
//            String[] spl = q.text().split(splr);
//            if(spl.length >= 2){
//                String subject = spl[0].trim();
//                String place = spl[1].trim();
//                Article pl = new ArticlesClient(new Session("", "telifie")).findPlace(place, params);
//                params.setLatitude( Double.parseDouble(pl.getAttribute("Longitude")));
//                params.setLongitude(Double.parseDouble(pl.getAttribute("Latitude")));
//                return new Document("$and", Arrays.asList(
//                    new Document("$or", Arrays.asList(
//                            new Document("tags", pattern(subject)),
//                            new Document("description", pattern(subject)),
//                            new Document("title", pattern(subject))
//                    )), new Document("location", new Document("$near", new Document("$geometry", new Document("type", "Point").append("coordinates", Arrays.asList(Double.parseDouble(pl.getAttribute("Longitude")), Double.parseDouble(pl.getAttribute("Latitude")))) ).append("$maxDistance", 16000)))
//                ));
//            }
//        }
//        List<Document> or = new ArrayList<>();
//        for (String word : q.split(" ")) {
//            or.add(new Document("title", pattern(word)));
//        }
//        or.add(new Document("tags", new Document("$in", Collections.singletonList(q))));
//        return new Document("$or", or);
        return new Document("title", Pattern.compile("^" + Pattern.quote(q), Pattern.CASE_INSENSITIVE));
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