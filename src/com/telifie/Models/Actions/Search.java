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
import org.bson.Document;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Pattern;

public class Search {

    private String query;
    private Statement statement;
    private Result result;
    private ArrayList<Article> results = new ArrayList<>();

    public Search(Configuration configuration, String query) {

        this.query = URLDecoder.decode(query, StandardCharsets.UTF_8).toLowerCase().trim();
        this.statement = new Statement(query);
        this.result = new Result(this.query);

        //TODO quick answers here if querying database is not necessary
        if(this.query.matches("(\\d+\\.?\\d*|\\.\\d+)([\\+\\-\\*\\/](\\d+\\.?\\d*|\\.\\d+))*")){ //Asking math expression

            Double mathResult = new DoubleEvaluator().evaluate(this.query);
            this.result.addQuickResult(
                    new CommonObject("https://telifie-static.nyc3.cdn.digitaloceanspaces.com/wwdb/calculate.gif",
                            String.valueOf(mathResult),
                            "", "Calculation")
            );

        }else if(this.query.matches("^(([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.){3}([01]?\\d\\d?|2[0-4]\\d|25[0-5])$")){

            UsersClient users = new UsersClient(configuration.getDomain(0));
            User user = users.getUserWithEmail(this.query);
            this.result.addQuickResult(
                    new CommonObject(
                            "",
                            user.getName(),
                            "", user.getEmail()
                    )
            );

        }else if(this.query.equals("how many articles are there")){
            //TODO if query is LIKE

        }

        try(MongoClient mongoClient = MongoClients.create(configuration.getDomain(0).getUri())){

            MongoDatabase database = mongoClient.getDatabase( (configuration.defaultDomain().getName().equals("telifie") || configuration.defaultDomain().getName().equals("") || configuration.defaultDomain().getName() == null ? "telifie" : "domains-articles") );
            MongoCollection<Document> collection = database.getCollection((configuration.defaultDomain().getName().equals("telifie") || configuration.defaultDomain().getName().equals("") || configuration.defaultDomain().getName() == null ? "articles" : configuration.defaultDomain().getName()));

            Document filter = generateFilter();
            FindIterable<Document> iterable = collection.find(filter).limit(100);

            for (Document document : iterable) {
                results.add(new Article(document));
            }
            this.result.setObject("articles");
            this.result.setCount(this.results.size());

            //Sort for query & name relevance
            Collections.sort(this.results, new RelevanceComparator(query));
            Collections.reverse(this.results);

            this.result.setResults(this.results.toString());

        }catch(MongoException e){

        }

    }

    /**
     * Generates MongoDB query filter based on query string.
     * Returns Document used for .find in for MongoCollection
     * @return Document
     */
    private Document generateFilter(){

        if(this.query.startsWith("id:")){ //Return articles with id

            return new Document("id", this.query.split(":")[1]);

        }else if(this.query.startsWith("description:")){ //Return Articles with requested description

            return new Document("description", pattern(this.query.split(":")[1]));

        }else if(this.query.startsWith("attribute:")){

            String[] spl = this.query.split(":"), spl2 = spl[1].split("=");
            String key = spl2[0].trim(), value = spl2[1].trim();

            return new Document("$and",
                    Arrays.asList(
                            new Document("attributes.key", pattern( key ) ),
                            new Document("attributes.value", pattern( value ) )
                    )
            );

        }else if(this.query.startsWith("define ")){
            //TODO
        }else{

            Pattern pattern = Pattern.compile(Pattern.quote(this.query), Pattern.CASE_INSENSITIVE);
            List<Document> orQuery = new ArrayList<Document>();
            orQuery.add(new Document("title", pattern));
            orQuery.add(new Document("link", pattern));
            orQuery.add(new Document("description", pattern));
            orQuery.add(new Document("tags", new Document("$in", Arrays.asList(this.query))));
            return new Document("$or", orQuery);

        }
        return null;//TODO remove when done with the former
    }

    public Result result(){
        return result;
    }

    private Pattern pattern(){
        return Pattern.compile(Pattern.quote(this.query), Pattern.CASE_INSENSITIVE);
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
