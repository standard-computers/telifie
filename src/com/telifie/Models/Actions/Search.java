package com.telifie.Models.Actions;

import com.fathzer.soft.javaluator.DoubleEvaluator;
import com.telifie.Models.Article;
import com.telifie.Models.Clients.ArticlesClient;
import com.telifie.Models.Clients.UsersClient;
import com.telifie.Models.Result;
import com.telifie.Models.User;
import com.telifie.Models.Articles.CommonObject;
import com.telifie.Models.Utilities.Configuration;
import com.telifie.Models.Utilities.Tool;
import org.bson.Document;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Pattern;

public class Search {

    public static Result execute(Configuration config, String query, Parameters params){

        query = URLDecoder.decode(query, StandardCharsets.UTF_8).toLowerCase().trim();
        return new Result(
                query,
                Search.quickResults(config, query),
                Search.executeQuery(config, query, params)
        );
    }

    private static ArrayList executeQuery(Configuration config, String query, Parameters params){

        String[] tokens = Parser.encoder.tokenize(query, true).get(0);
        String cleaned = Parser.encoder.clean(query);

        //Filters to search by, making it flexible ladies
        ArrayList<Document> filters = new ArrayList<>();

        //Always search by titles and links
        filters.add(new Document("title", new Document("$in",
                Arrays.asList(
                        pattern(cleaned),
                        pattern(query))
                )
            )
        );
        filters.add(new Document("link", new Document("$in",
                Arrays.asList(
                        pattern(cleaned),
                        pattern(query))
                )
            )
        );

        //Do specialize query filters for uncleaned query ONLY
        if(generalFilter(query) != null){
            filters.add(generalFilter(query));
        }
        for(String token : tokens){
            String[] properties = {"link", "title", "description"};
            for(String property : properties){
                filters.add(new Document(property, pattern(token) ) );
            }
        }
        filters.add(new Document("tags", new Document("$in", Arrays.asList(tokens)) ) );

        ArticlesClient articles = new ArticlesClient(config);
        ArrayList<Article> results = articles.search(config, params, Search.filter(filters));

        //Sort for query & name relevance
        if(results != null && results.size() > 3){
            Collections.sort(results, new RelevanceComparator(cleaned));
            Collections.reverse(results);
        }
        return results;
    }

    /**
     * Generates MongoDB query filter based on query string.
     * Returns Document used for .find in for MongoCollection
     * @return Document
     */
    private static Document generalFilter(String query){

        if(query.matches("^id\\s*:\\s*.*")){ //Return articles with id

            String[] spl = query.split(":");
            if(spl.length >= 2) {

                return new Document("id", spl[1].trim());
            }
        }else if(query.matches("^description\\s*:\\s*.*")){ //Return Articles with requested description

            String[] spl = query.split(":");
            if(spl.length >= 2) {

                return new Document("description", pattern(spl[1].trim()));
            }
        }else if(query.matches("^title\\s*:\\s*.*")){ //Return Articles with requested description

            String[] spl = query.split(":");
            if(spl.length >= 2){

                return new Document("title", pattern(query.split(":")[1]));
            }
        }else if(query.matches("^attribute\\s*:\\s*.*")){

            String[] spl = query.split(":");
            if(spl.length >= 2){

                String[] spl2 = spl[1].split("=");
                if(spl2.length < 2){

                    if(query.contains("&")){ //Has multiple attribute requirements

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

                        String key = spl2[0].trim();
                        if(spl2.length >= 2){

                            String value = spl2[1].trim();
                            return new Document("$and",
                                    Arrays.asList(
                                            new Document("attributes.key", pattern( key ) ),
                                            new Document("attributes.value", pattern( value ) )
                                    )
                            );
                        }else{

                            return new Document("attributes.key", pattern( key ) );
                        }
                    }
                }
            }

        }else if(query.startsWith("define ")) {

            return new Document("$and",
                    Arrays.asList(
                            new Document("description", pattern("definition")),
                            new Document("title", query.replaceFirst("define ", ""))
                    )
            );
        }
        return null;
    }

    /**
     * Wrapper method for building document filters
     * @param filters
     * @return Document filter for actual Search query
     */
    private static Document filter(ArrayList filters){
        return new Document("$or", filters);
    }

    private static ArrayList<CommonObject> quickResults(Configuration config, String query){

        ArrayList<CommonObject> quickResults = new ArrayList<>();
        if(query.matches("(\\d+\\.?\\d*|\\.\\d+)([\\+\\-\\*\\/](\\d+\\.?\\d*|\\.\\d+))*") || Tool.containsAnyOf(new String[] {"+", "-", "*", "/", "^"}, query)){ //Asking math expression

            try {
                Double mathResult = new DoubleEvaluator().evaluate(query.replaceAll("\\$", ""));
                quickResults.add(
                        new CommonObject("https://telifie-static.nyc3.cdn.digitaloceanspaces.com/wwdb/calculate.gif",
                                Tool.formatNumber(mathResult),
                                "", "Calculation")
                );
            }catch(IllegalArgumentException e){}
        }
        if(query.matches("^(([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.){3}([01]?\\d\\d?|2[0-4]\\d|25[0-5])$")){

            UsersClient users = new UsersClient(config);
            User user = users.getUserWithEmail(query);
            quickResults.add(
                    new CommonObject(
                            "",
                            user.getName(),
                            "",
                            user.getEmail()
                    )
            );
        }
        if(query.equals("how many articles are there")){
            //TODO if query is LIKE
        }
        if(Tool.isHexColor(query) || Tool.isHSLColor(query) || Tool.isRGBColor(query)){

            quickResults.add(
                    new CommonObject(
                            "COLOR_ICON",
                            query,
                            "",
                            query
                    )
            );
        }

        if(query.contains("random") && query.contains("color")){

            //TODO https://www.thecolorapi.com/
        }
        return quickResults;
    }

    private static Pattern pattern(String value){
        String regex = "\\b" + Pattern.quote(value) + "\\w*\\b";
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
