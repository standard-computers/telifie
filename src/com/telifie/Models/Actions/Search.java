package com.telifie.Models.Actions;

import com.mongodb.client.model.Filters;
import com.telifie.Models.Andromeda;
import com.telifie.Models.Article;
import com.telifie.Models.Clients.ArticlesClient;
import com.telifie.Models.Result;
import com.telifie.Models.Utilities.Parameters;
import com.telifie.Models.Utilities.Session;
import com.telifie.Models.Utilities.Telifie;
import org.bson.Document;
import org.bson.conversions.Bson;
import java.util.*;
import java.util.regex.Pattern;

public class Search {

    private ArticlesClient articles;

    public Result execute(Session session, String query, Parameters params){
        query = query.toLowerCase().trim();
        articles = new ArticlesClient(session);
        ArrayList<Article> results = articles.search(query, params, filter(query, params));
        ArrayList<Article> paged = paginateArticles(results, params.getPage(), params.getResultsPerPage());
        Result r = new Result(query, params, "articles", paged);
        r.setTotal(results.size());
        r.setGenerated(this.generated(query));
        return r;
    }

    private Document filter(String query, Parameters params){

        if(query.matches("^id\\s*:\\s*.*")){

            String[] spl = query.split(":");
            if(spl.length >= 2) {
                return new Document("id", spl[1].trim());
            }
        }else if(query.matches("^description\\s*:\\s*.*")){

            String[] spl = query.split(":");
            if(spl.length >= 2) {
                return new Document("description", Pattern.compile("\\b" + Pattern.quote(spl[1].trim()) + "\\b", Pattern.CASE_INSENSITIVE));
            }
        }else if(query.matches("^title\\s*:\\s*.*")){

            String[] spl = query.split(":");
            if(spl.length >= 2){
                return new Document("title", pattern(spl[1].trim() ));
            }
        }else if(query.matches("^link\\s*:\\s*.*")){

            String[] spl = query.split(":");
            if(spl.length >= 2){
                return new Document("link", pattern(spl[1].trim() ));
            }
        }else if(query.matches("^source\\s*:\\s*.*")){

            String[] spl = query.split(":");
            if(spl.length >= 2){
                if(spl[1].startsWith("http")){
                    return new Document("source.url", spl[1].trim());
                }
                return new Document("source.name", pattern(spl[1].trim() ));
            }
        }else if(query.matches("^attribute\\s*:\\s*.*")){

            String[] spl = query.split(":");
            if(spl.length >= 2){
                String[] spl2 = spl[1].split("=");
                String key = spl2[0].trim().toLowerCase();
                if(spl2.length >= 2){
                    String value = spl2[1].trim().toLowerCase();
                    return new Document("attributes", new Document("$elemMatch", new Document("key", Pattern.compile(Pattern.quote(key), Pattern.CASE_INSENSITIVE))
                            .append("value", Pattern.compile(Pattern.quote(value), Pattern.CASE_INSENSITIVE))));
                }
                return new Document("attributes.key", Pattern.compile(Pattern.quote(key), Pattern.CASE_INSENSITIVE));
            }
        }else if(query.matches("^define\\s*.*")) {

            return new Document("$and", Arrays.asList(new Document("description", "Definition"), new Document("title", pattern(query.replaceFirst("define", "").trim()))));
        }else if(query.matches("(?i)\\bhttps?://\\S+\\b")){

            return new Document("link", new Document("$in", Arrays.asList(pattern(query), pattern(query))));
        }else if(query.matches("^(\\d+)\\s+([A-Za-z\\s]+),\\s+([A-Za-z\\s]+),\\s+([A-Za-z]{2})\\s+(\\d{5})$")){

            return new Document("$and", Arrays.asList(new Document("attribute.key", "Address"), new Document("attribute.value", pattern(query)) ) );
        }else if(query.matches("^\\+\\d{1,3}\\s*\\(\\d{1,3}\\)\\s*\\d{3}-\\d{4}$")){

            return new Document("$and", Arrays.asList( new Document("attribute.key", "Phone"), new Document("attribute.value", query) ) );
        }else if (query.matches("^\\w+@\\w+\\.[a-zA-Z]{2,3}$")) {

            return new Document("$and", Arrays.asList( new Document("attribute.key", "Email"), new Document("attribute.value", query.toLowerCase()) ));
        }else if(query.matches("\\b(\\d{4}-\\d{2}-\\d{2}|\\d{2}/\\d{2}/\\d{4}|\\d{2}-[a-zA-Z]{3}-\\d{4}|[a-zA-Z]+ \\d{1,2}, \\d{4})\\b")){
            return new Document("$and", Arrays.asList(
                    new Document("attribute.key", new Document("$in", Arrays.asList(
                            ignoreCase("date"),
                            ignoreCase("founded"),
                            ignoreCase("established"),
                            ignoreCase("started")
                    ))),
                    new Document("attribute.value", query)
            ));
        }else if(query.endsWith("near me")){

            String q = query.replace("near me", "").trim();
            return new Document("$and", Arrays.asList(
                    new Document("$or", Arrays.asList(
                            new Document("tags", pattern(q)),
                            new Document("description", pattern(q)),
                            new Document("title", pattern(q))
                    )),
                    new Document("location", new Document("$near",
                            new Document("$geometry", new Document("type", "Point")
                                .append("coordinates", Arrays.asList( params.getLongitude(), params.getLatitude() ))
                            ).append("$maxDistance", 16000)
                    )
                )
            ));
        }else if(Telifie.tools.has(Andromeda.PROXIMITY, query) > -1){
            String splr = Andromeda.PROXIMITY[Telifie.tools.has(Andromeda.PROXIMITY, query)];
            String[] spl = query.split(splr);
            if(spl.length >= 2){
                String subject = spl[0].trim();
                String place = spl[1].trim();
                ArrayList<Article> findPlace = articles.get(
                        new Document("$and", Arrays.asList(
                                new Document("title", pattern(place)),
                                new Document("description", pattern("city")),
                                new Document("location", new Document("$near",
                                        new Document("$geometry", new Document("type", "Point")
                                                .append("coordinates", Arrays.asList(
                                                        params.getLongitude(),
                                                        params.getLatitude()
                                                ))
                                        ).append("$maxDistance", Integer.MAX_VALUE)
                                )
                                )
                        ))
                );
                Article pl = findPlace.get(0);
                params.setLatitude( Double.parseDouble(pl.getAttribute("Longitude")));
                params.setLongitude(Double.parseDouble(pl.getAttribute("Latitude")));
                return new Document("$and", Arrays.asList(
                        new Document("$or", Arrays.asList(
                                new Document("tags", pattern(subject)),
                                new Document("description", pattern(subject)),
                                new Document("title", pattern(subject))
                        )),
                        new Document("location", new Document("$near",
                                new Document("$geometry", new Document("type", "Point")
                                        .append("coordinates", Arrays.asList(
                                                Double.parseDouble(pl.getAttribute("Longitude")),
                                                Double.parseDouble(pl.getAttribute("Latitude"))
                                        ))
                                ).append("$maxDistance", 16000)
                        )
                        )
                ));
            }
        }
        String[] exploded = Andromeda.encoder.nova(Andromeda.encoder.clean(query));
        ArrayList<Bson> or = new ArrayList<>();
        for (String word : exploded) {
            or.add(Filters.regex("title", pattern(word)));
        }
        if (query.contains("/") || query.contains(".")) {
            or.add(Filters.regex("link", pattern(query)));
        }
        or.add(Filters.in("tags", ignoreCase(query)));
        Bson sf = Filters.ne("description", "Image");
        if(params.getIndex().equals("images")){
            sf = Filters.eq("description", "Image");
        }else if(params.getIndex().equals("locations")){
            sf = Filters.exists("location");
        }
        return new Document("$and", Arrays.asList( sf, Filters.or(or) ));
    }

    private String generated(String q){
        if((q.contains("*") || q.contains("/") || q.contains("+") || q.contains("-")) && Telifie.tools.contains(Andromeda.NUMERALS, q)){

        }
        return null;
    }

    private ArrayList<Article> paginateArticles(ArrayList<Article> results, int page, int pageSize) {
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

    private static Pattern pattern(String value){
        return Pattern.compile("\\b" + Pattern.quote(value) + "\\w*\\b", Pattern.CASE_INSENSITIVE);
    }

    private static Pattern ignoreCase(String value) { //Doesn't allow extra stuff at end
        return Pattern.compile("\\b" + Pattern.quote(value) + "\\b", Pattern.CASE_INSENSITIVE);
    }
}