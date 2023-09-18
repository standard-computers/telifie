package com.telifie.Models.Actions;

import com.mongodb.client.model.Filters;
import com.telifie.Models.Andromeda;
import com.telifie.Models.Article;
import com.telifie.Models.Articles.Image;
import com.telifie.Models.Clients.ArticlesClient;
import com.telifie.Models.Result;
import com.telifie.Models.Utilities.Configuration;
import com.telifie.Models.Utilities.Parameters;
import com.telifie.Models.Utilities.Session;
import com.telifie.Models.Utilities.Telifie;
import org.bson.Document;
import org.bson.conversions.Bson;
import java.util.*;
import java.util.regex.Pattern;

public class Search {

    private static ArticlesClient articlesClient;
    private Parameters params;

    public Result execute(Configuration config, Session session, String query, Parameters params){
        query = query.toLowerCase().trim();
        this.params = params;
        articlesClient = new ArticlesClient(config, session);
        ArrayList results = executeQuery(config, session, query);
        switch (params.getIndex()) {
            case "images" -> {
                ArrayList<Image> images = new ArrayList<>();
                for (Object result : results) {
                    Article article = (Article) result;
                    ArrayList<Image> articleImages = article.getImages();
                    if (articleImages != null && !articleImages.isEmpty()) {
                        for (Image image : articleImages) {
                            images.add(image);
                        }
                    }
                }
                return new Result(query, params, "images", images);
            }
            case "locations" -> {
                ArrayList<Article> articles = new ArrayList<>();
                for (Object result : results) {
                    Article article = (Article) result;
                    if (article.hasAttribute("latitude") && article.hasAttribute("longitude")) {
                        articles.add(article);
                    }
                }
                return new Result(query, params, "articles", articles);
            }
            case "shopping" -> {
                ArrayList<Article> articles = new ArrayList<>();
                for (Object result : results) {
                    Article article = (Article) result;
                    if (article.hasAttribute("cost") || article.hasAttribute("price") || article.hasAttribute("value")) {
                        articles.add(article);
                    }
                }
                return new Result(query, params,"articles", articles);
            }
        }
        ArrayList<Article> qr = new ArrayList<>();
        return new Result(query, params, qr, results);
    }

    private ArrayList executeQuery(Configuration config, Session session, String query){

        Document filter = filter(query);
        if(!session.getDomain().equals("telifie")){
            ArrayList<Document> conditions = new ArrayList<>();
            String domainId = config.getDomain().getId();
            conditions.add(new Document("domain", domainId));
            Document queryFilter = filter(query);
            conditions.add(queryFilter);
            filter = new Document("$and", conditions);
        }
        ArrayList<Article> results = articlesClient.search(params, filter);
        if(results != null && !query.contains(":")){
            if(Telifie.tools.has(Telifie.PROXIMITY, query) > -1 || query.endsWith("near me")) {
                Collections.sort(results, new DistanceSorter(params.getLatitude(), params.getLongitude()));
            }else if(query.split(" ").length > 2 && !query.contains(",")){
                Collections.sort(results, new CosmoScore(Andromeda.encoder.clean(query)));
            }else{
                Collections.sort(results, new RelevanceComparator(query));
                Collections.reverse(results);
            }
        }
        return results;
    }

    private Document filter(String query){

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
                    if(spl2.length >= 2){
                        String value = spl2[1].trim().toLowerCase();
                        return new Document("$and", Arrays.asList(
                                new Document("attributes.key", wholeWord(key)),
                                new Document("attributes.value", wholeWord(value))
                        ));
                    }
                }
                return new Document("attributes.key", wholeWord(key));
            }
        }else if(query.matches("^define\\s*.*")) {

            String term = query.replaceFirst("define", "").trim();
            return new Document("$and", Arrays.asList(
                    new Document("description", "Definition"),
                    new Document("title", pattern(term))
            ));
        }else if(query.matches("(?i)\\bhttps?://\\S+\\b")){

            return new Document("link", new Document("$in", Arrays.asList(pattern(query), pattern(query))));
        }else if(query.matches("^(\\d+)\\s+([A-Za-z\\s]+),\\s+([A-Za-z\\s]+),\\s+([A-Za-z]{2})\\s+(\\d{5})$")){

            return new Document("$and", Arrays.asList(
                    new Document("attribute.key", "Address"),
                    new Document("attribute.value", pattern(query))
            ));
        }else if(query.matches("^\\+\\d{1,3}\\s*\\(\\d{1,3}\\)\\s*\\d{3}-\\d{4}$")){

            return new Document("$and", Arrays.asList(
                    new Document("attribute.key", "Phone"),
                    new Document("attribute.value", query) //adjust, format query input
            ));
        }else if (query.matches("^\\w+@\\w+\\.[a-zA-Z]{2,3}$")) {

            return new Document("$and", Arrays.asList(
                    new Document("attribute.key", "Email"),
                    new Document("attribute.value", query.toLowerCase()) //adjust, format query input
            ));
        }else if(query.matches("\\b(\\d{4}-\\d{2}-\\d{2}|\\d{2}/\\d{2}/\\d{4}|\\d{2}-[a-zA-Z]{3}-\\d{4}|[a-zA-Z]+ \\d{1,2}, \\d{4})\\b")){
            return new Document("$and", Arrays.asList(
                    new Document("attribute.key", new Document("$in", Arrays.asList(
                            ignoreCase("date"),
                            ignoreCase("founded"),
                            ignoreCase("established"),
                            ignoreCase("started")
                    ))),
                    new Document("attribute.value", query) //adjust, format query input
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
                                    .append("coordinates", Arrays.asList(
                                            params.getLongitude(),
                                            params.getLatitude()
                                    ))
                            ).append("$maxDistance", 16000)
                    )
                    )
            ));
        }else if(Telifie.tools.has(Telifie.PROXIMITY, query) > -1){
            String splr = Telifie.PROXIMITY[Telifie.tools.has(Telifie.PROXIMITY, query)];
            String[] spl = query.split(splr);
            if(spl.length >= 2){
                String subject = spl[0].trim();
                String place = spl[1].trim();
                ArrayList<Article> findPlace = articlesClient.get(
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
        String cleanedQuery = Andromeda.encoder.clean(query);
        String[] exploded = Andromeda.encoder.nova(cleanedQuery);
        ArrayList<Bson> filters = new ArrayList<>();
        List<Bson> titleWordFilters = new ArrayList<>();
        titleWordFilters.add(Filters.regex("title", ignoreCase(query)));
        for (String word : exploded) {
            titleWordFilters.add(Filters.regex("title", ignoreCase(word)));
        }
        if (!titleWordFilters.isEmpty()) {
            filters.add(Filters.or(titleWordFilters));
        }
        filters.add(Filters.regex("link", pattern(query)));
        filters.add(Filters.in("tags", ignoreCase(query)));
        return new Document("$or", filters);
    }

    private static Pattern wholeWord(String value){
        return Pattern.compile(Pattern.quote(value), Pattern.CASE_INSENSITIVE);
    }

    private static Pattern pattern(String value){
        return Pattern.compile("\\b" + Pattern.quote(value) + "\\w*\\b", Pattern.CASE_INSENSITIVE);
    }

    private static Pattern ignoreCase(String value) { //Doesn't allow extra stuff at end
        return Pattern.compile("\\b" + Pattern.quote(value) + "\\b", Pattern.CASE_INSENSITIVE);
    }

    private static class CosmoScore implements Comparator<Article> {

        private final String query;
        private final String[] words;

        public CosmoScore(String query){
            this.query = query;
            this.words = Andromeda.encoder.nova(Andromeda.encoder.clean(query));
        }

        @Override
        public int compare(Article a, Article b) {
            double relevanceA = relevance(a);
            double relevanceB = relevance(b);
            return Double.compare(relevanceB, relevanceA);
        }

        private double relevance(Article a) {
            if(a.getTitle().trim().toLowerCase().equals(query)){
                return Integer.MAX_VALUE;
            }
            double titleGrade = (countMatches(a.getTitle(), words) / words.length) * 4;
            double linkGrade = (countMatches((a.getLink() == null ? "" : a.getLink()), words) / words.length) * 2;
            double tagsGrade = 0;
            if(a.getTags() != null && !a.getTags().isEmpty()){
                for(String tag : a.getTags()){
                    tagsGrade += countMatches(tag, words);
                }
            }
            return ((titleGrade + linkGrade) + ((tagsGrade / words.length) * 0.25)) * a.getPriority();
        }

        private double countMatches(String text, String[] words) {
            int matches = 0;
            for(String word : words) {
                if(text.contains(word)) {
                    matches++;
                }
            }
            return matches / words.length;
        }
    }

    private static class RelevanceComparator implements Comparator<Article> {

        private String query;

        public RelevanceComparator(String query) {
            this.query = query;
        }

        @Override
        public int compare(Article a, Article b) {
            int relevanceA = relevance(a.getTitle());
            int relevanceB = relevance(b.getTitle());
            return Integer.compare(relevanceB, relevanceA);
        }

        private int relevance(String title) {
            int lenDiff = Math.abs(title.length() - query.length());
            if (lenDiff > 3) {
                return lenDiff;
            }
            int[] prev = new int[query.length() + 1];
            int[] curr = new int[query.length() + 1];
            for (int i = 0; i <= title.length(); i++) {
                for (int j = 0; j <= query.length(); j++) {
                    if (i == 0) {
                        curr[j] = j;
                    } else if (j == 0) {
                        curr[j] = i;
                    } else {
                        curr[j] = Math.min(prev[j - 1] + (title.charAt(i - 1) == query.charAt(j - 1) ? 0 : 1),
                                Math.min(prev[j], curr[j - 1]) + 1);
                    }
                }
                int[] temp = prev;
                prev = curr;
                curr = temp;
            }
            return prev[query.length()];
        }
    }

    private static class DistanceSorter implements Comparator<Article> {

        private final double latitude, longitude;

        public DistanceSorter(double latitude, double longitude) {
            this.latitude = latitude;
            this.longitude = longitude;
        }

        @Override
        public int compare(Article a, Article b) {
            double relevanceA = distance(Double.parseDouble(a.getAttribute("Latitude")), Double.parseDouble(a.getAttribute("Longitude")));
            double relevanceB = distance(Double.parseDouble(b.getAttribute("Latitude")), Double.parseDouble(b.getAttribute("Longitude")));
            return Double.compare(relevanceB, relevanceA);
        }

        private double distance(double latitude, double longitude) {
            final int R = 6371; // Radius of the earth in km
            double latDistance = Math.toRadians(latitude - this.latitude);
            double lonDistance = Math.toRadians(longitude - this.longitude);
            double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2) + Math.cos(Math.toRadians(this.latitude)) * Math.cos(Math.toRadians(this.longitude)) * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
            double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
            return R * c;
        }
    }
}