package com.telifie.Models.Actions;

import com.mongodb.client.model.Filters;
import com.telifie.Models.Andromeda;
import com.telifie.Models.Article;
import com.telifie.Models.Articles.Image;
import com.telifie.Models.Clients.ArticlesClient;
import com.telifie.Models.Result;
import com.telifie.Models.Utilities.Configuration;
import com.telifie.Models.Utilities.Parameters;
import org.bson.Document;
import org.bson.conversions.Bson;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Pattern;

public class Search {

    public static Result execute(Configuration config, String query, Parameters params){

        query = URLDecoder.decode(query, StandardCharsets.UTF_8);

        //Quick results
        String generated = "";
        if(query.matches("([^\\s]+(?:\\s+[^\\s]+)*) of ([^\\s]+(?:\\s+[^\\s]+)*)")){
            String[] spl = query.split("of");
            if(spl.length >= 2){
                String info = spl[0].trim();
                String subject = spl[1].trim();
                ArticlesClient articles = new ArticlesClient(config);
                ArrayList<Article> r = articles.get(new Document("title", pattern(subject)));
                if(r.size() > 0) {
                    Article p = r.get(0);
                    String answer = p.getAttribute(info);
                    if (answer != null) {
                        generated = "The **" + info + "** of " + p.getTitle() + " is **" + answer + "**";
                        query = subject;
                    }
                }
            }
        }

        ArrayList results = Search.executeQuery(config, query, params);

        if(params.getIndex().equals("images")) {
            ArrayList<Image> images = new ArrayList();
            for(Object result : results){
                Article article = (Article) result;
                ArrayList<Image> articleImages = article.getImages();
                if(articleImages != null && articleImages.size() > 0){
                    for(Image image : articleImages){
                        image.setId(article.getId());
                        images.add(image);
                    }
                }
            }
            return new Result(query, "images", images);
        }else if(params.getIndex().equals("locations")){
            ArrayList<Article> articles = new ArrayList();
            for(Object result : results){
                Article article = (Article) result;
                if(article.hasAttribute("latitude") && article.hasAttribute("longitude")){
                    articles.add(article);
                }
            }
            return new Result(query, "articles", articles);
        }else if(params.getIndex().equals("shopping")){
            ArrayList<Article> articles = new ArrayList();
            for(Object result : results){
                Article article = (Article) result;
                if(article.hasAttribute("cost") || article.hasAttribute("price") || article.hasAttribute("value")){
                    articles.add(article);
                }
            }
            return new Result(query, "articles", articles);
        }
        ArrayList<Article> qr = new ArrayList<>();
        return new Result(query, qr, results, generated);
    }

    private static ArrayList executeQuery(Configuration config, String query, Parameters params){

        ArticlesClient articles = new ArticlesClient(config);
        ArrayList<Article> results = articles.search(config, params, filter(query, params));
        if(results != null && results.size() > 3){
            Collections.sort(results, new RelevanceComparator(query));
            Collections.reverse(results);
        }
        return results;
    }

    /**
     * Generates MongoDB query filter based on query string.
     * Returns Document used for .find in for MongoCollection
     * @return Document
     */
    private static Document filter(String query, Parameters params){

        if(query.matches("^id\\s*:\\s*.*")){ //Return articles with id

            String[] spl = query.split(":");
            if(spl.length >= 2) {
                return new Document("id", spl[1].trim());
            }
        }else if(query.matches("^description\\s*:\\s*.*")){

            String[] spl = query.split(":");
            if(spl.length >= 2) {
                return new Document("description", pattern(spl[1].trim()));
            }
        }else if(query.matches("^title\\s*:\\s*.*")){

            String[] spl = query.split(":");
            if(spl.length >= 2){
                return new Document("title", pattern(spl[1].trim() ));
            }
        }else if(query.matches("^source\\s*:\\s*.*")){

            String[] spl = query.split(":");
            if(spl.length >= 2){
                if(spl[1].startsWith("http")){
                    return new Document("source.url", pattern(spl[1].trim() ));
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
                                new Document("attributes.value", pattern(value))
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
        }else if(query.matches("^(.+)\\s+near me$")){

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
        }
        Andromeda.unit tokenized = Andromeda.encoder.tokenize(query, true).get(0);
        ArrayList<Bson> filters = new ArrayList<>();
        Bson[] titleFilters = {
                Filters.regex("title", pattern(Andromeda.encoder.clean(query))),
                Filters.regex("title", pattern(query))
        };
        filters.add(Filters.or(titleFilters));
        filters.add(Filters.regex("link", pattern(query)));
        filters.add(Filters.in("tags", tokenized.tokens()));
        filters.add(Filters.regex("attributes.value", pattern(query)));
        return new Document("$or", filters);
    }

    private static Pattern wholeWord(String value){
        return Pattern.compile(Pattern.quote(value), Pattern.CASE_INSENSITIVE);
    }

    private static Pattern pattern(String value){
        String regex = "\\b" + Pattern.quote(value) + "\\w*\\b";
        return Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
    }

    private static Pattern ignoreCase(String value) {
        String regex = "\\b" + Pattern.quote(value) + "\\b";
        return Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
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
            int[][] dp = new int[title.length() + 1][query.length() + 1];
            for (int i = 0; i <= title.length(); i++) {
                for (int j = 0; j <= query.length(); j++) {
                    if (i == 0) {
                        dp[i][j] = j;
                    } else if (j == 0) {
                        dp[i][j] = i;
                    } else {
                        dp[i][j] = Math.min(dp[i - 1][j - 1] + (title.charAt(i - 1) == query.charAt(j - 1) ? 0 : 1),
                                Math.min(dp[i - 1][j], dp[i][j - 1]) + 1);
                    }
                }
            }
            return dp[title.length()][query.length()];
        }
    }
}