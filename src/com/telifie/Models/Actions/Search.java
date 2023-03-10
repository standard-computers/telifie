package com.telifie.Models.Actions;

import com.fathzer.soft.javaluator.DoubleEvaluator;
import com.mongodb.MongoException;
import com.mongodb.client.*;
import com.telifie.Models.Article;
import com.telifie.Models.Clients.UsersClient;
import com.telifie.Models.Result;
import com.telifie.Models.User;
import com.telifie.Models.Utilities.CommonObject;
import com.telifie.Models.Utilities.Configuration;
import com.telifie.Models.Utilities.Statement;
import com.telifie.Models.Utilities.Tool;
import org.bson.Document;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Pattern;

public class Search {

    private String query, targetDomain, domainArticles;
    private Statement statement;
    private Result result;

    public Search(Configuration configuration, String query) {

        this.query = URLDecoder.decode(query, StandardCharsets.UTF_8).toLowerCase().trim();
        this.statement = new Statement(this.query);
        this.result = new Result(this.query);
        this.targetDomain = (configuration.defaultDomain().getName().equals("telifie") || configuration.defaultDomain().getName().equals("") || configuration.defaultDomain().getName() == null ? "telifie" : "domains-articles");
        this.domainArticles = (configuration.defaultDomain().getName().equals("telifie") || configuration.defaultDomain().getName().equals("") || configuration.defaultDomain().getName() == null ? "articles" : configuration.defaultDomain().getName());
        //TODO integrate ArticlesClient

        if(this.query.matches("(\\d+\\.?\\d*|\\.\\d+)([\\+\\-\\*\\/](\\d+\\.?\\d*|\\.\\d+))*") || Tool.containsAnyOf(new String[] {"+", "-", "*", "/", "^"}, this.query)){ //Asking math expression

            try {
                Double mathResult = new DoubleEvaluator().evaluate(this.query.replaceAll("\\$", ""));
                this.result.addQuickResult(
                        new CommonObject("https://telifie-static.nyc3.cdn.digitaloceanspaces.com/wwdb/calculate.gif",
                                Tool.formatNumber(mathResult),
                                "", "Calculation")
                );
            }catch(IllegalArgumentException e){

            }
        }else if(this.query.matches("^(([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.){3}([01]?\\d\\d?|2[0-4]\\d|25[0-5])$")){

            UsersClient users = new UsersClient(configuration.getDomain(0));
            User user = users.getUserWithEmail(this.query);
            this.result.addQuickResult(
                new CommonObject(
                    "",
                    user.getName(),
                    "",
                    user.getEmail()
                )
            );
        }else if(this.query.equals("how many articles are there")){
            //TODO if query is LIKE

        }else if(Tool.isHexColor(this.query) || Tool.isHSLColor(this.query) || Tool.isRGBColor(this.query)){

            this.result.addQuickResult(
                new CommonObject(
                    "COLOR_ICON",
                    query,
                    "",
                    query
                )
            );
        }else if(this.query.contains("random") && this.query.contains("color")){

            //TODO https://www.thecolorapi.com/

        }

        try(MongoClient mongoClient = MongoClients.create(configuration.getDomain(0).getUri())){

            MongoDatabase database = mongoClient.getDatabase(targetDomain);
            MongoCollection<Document> collection = database.getCollection(domainArticles);
            FindIterable<Document> iterable = collection.find(generateFilter()).limit(100);

            ArrayList<Article> results = new ArrayList<>();
            for (Document document : iterable) {
                results.add(new Article(document));
            }
            this.result.setObject("articles");
            this.result.setCount(results.size());

            //Sort for query & name relevance
            Collections.sort(results, new RelevanceComparator(this.query));
            Collections.reverse(results);

            this.result.setResults(results.toString());

        }catch(MongoException e){

        }
    }

    /**
     * Generates MongoDB query filter based on query string.
     * Returns Document used for .find in for MongoCollection
     * @return Document
     */
    private Document generateFilter(){

        if(this.query.matches("^id\\s*:\\s*.*")){ //Return articles with id

            String[] spl = this.query.split(":");
            if(spl.length >= 2) {

                return new Document("id", spl[1]);
            }
            return generalFilter();
        }else if(this.query.matches("^description\\s*:\\s*.*")){ //Return Articles with requested description

            return new Document("description", pattern(this.query.split(":")[1]));

        }else if(this.query.matches("^title\\s*:\\s*.*")){ //Return Articles with requested description

            String[] spl = this.query.split(":");
            if(spl.length >= 2){

                return new Document("title", pattern(this.query.split(":")[1]));
            }
            return generalFilter();
        }else if(this.query.matches("^attribute\\s*:\\s*.*")){

            String[] spl = this.query.split(":"), spl2 = spl[1].split("=");
            if(spl[1].contains("&")){ //Has multiple attribute requirements

                String[] attrReqs = spl[1].split("&");
                List<Document> andFilters = new ArrayList<>();
                for (String attr : attrReqs) {

                    String[] args = attr.split("=");
                    String key = args[0].trim(), value = args[1].trim();
                    andFilters.add(new Document("attributes.key", pattern(key)));
                    andFilters.add(new Document("attributes.value", pattern(value)));
                }

                return new Document("$and", andFilters);
            }else{

                String key = spl2[0].trim(), value = spl2[1].trim();
                return new Document("$and",
                        Arrays.asList(
                                new Document("attributes.key", pattern( key ) ),
                                new Document("attributes.value", pattern( value ) )
                        )
                );
            }

        }else if(this.query.startsWith("define ")) {

            return new Document("$and",
                    Arrays.asList(
                            new Document("description", pattern("definition")),
                            new Document("title", pattern(this.query.replaceFirst("define ", "")))
                    )
            );
        }
        return generalFilter();
    }

    private Document generalFilter(){
        Pattern pattern = Pattern.compile(Pattern.quote(this.query), Pattern.CASE_INSENSITIVE);
        return new Document("$or",
                Arrays.asList(
                        new Document("title", pattern),
                        new Document("link", pattern),
                        new Document("description", pattern),
                        new Document("tags", new Document("$in", Arrays.asList(this.query)))
                )
        );
    }

    public Result result(){
        return result;
    }

    private Pattern pattern(String value){
        return Pattern.compile(Pattern.quote(value), Pattern.CASE_INSENSITIVE);
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
            // Calculate the Levenshtein distance between the title and the query
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
