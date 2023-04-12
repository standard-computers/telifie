package com.telifie.Models.Clients;

import com.mongodb.BasicDBObject;
import com.mongodb.MongoException;
import com.mongodb.client.*;
import com.telifie.Models.Actions.Out;
import com.telifie.Models.Actions.Parameters;
import com.telifie.Models.Article;
import com.telifie.Models.Utilities.Configuration;
import org.bson.Document;
import java.util.ArrayList;
import java.util.Arrays;

public class ArticlesClient extends Client {

    public ArticlesClient(Configuration config){
        super(config);
        super.collection = "articles";
    }

    public boolean update(Article article, Article newArticle){
        Out.console(newArticle.toString());
        return super.updateOne(
                new Document("id", article.getId()),
                new Document("$set", Document.parse(newArticle.toString()))
        );
    }

    public boolean create(Article article){
        return super.insertOne(Document.parse(article.toString()));
    }

    public Article get(String articleId){
        return new Article(this.findOne(new Document("id", articleId)));
    }

    public boolean verify(String articleId){
        return this.updateOne(new Document("id", articleId), new Document("$set", new Document("verified", true)));
    }

    public ArrayList<Article> get(Document filter){
        ArrayList<Document> found = this.find(filter);
        ArrayList<Article> articles = new ArrayList<>();
        for(Document doc : found){
            articles.add(new Article(doc));
        }
        return articles;
    }


    public ArrayList<Article> getArticlesWithAttribute(String key, String value){
        ArrayList<Document> found = this.find(
            new Document("$and",
                Arrays.asList(
                        new Document("attributes.key", key ),
                        new Document("attributes.value", value )
                )
            )
        );
        ArrayList<Article> articles = new ArrayList<>();
        for(Document doc : found){
            articles.add(new Article(doc));
        }
        return articles;
    }

    public ArrayList<Article> search(Configuration config, Parameters params, Document filter){
        String domainName = config.getDomain().getName();
        String targetDomain = (domainName.equals("telifie") || domainName.equals("") ? "telifie" : "domains-articles");
        String domainArticles = (domainName.equals("telifie") || domainName.equals("") ? "articles" : config.getDomain().getName());
        try(MongoClient mongoClient = MongoClients.create(config.getDomain().getUri())){
            MongoDatabase database = mongoClient.getDatabase(targetDomain);
            MongoCollection<Document> collection = database.getCollection(domainArticles);
            FindIterable<Document> iterable = collection.find(filter)
                    .sort(new BasicDBObject("priority", -1))
                    .skip(params.getSkip())
                    .limit(params.getResultsPerPage());
            ArrayList<Article> results = new ArrayList<>();
            for (Document document : iterable) {
                results.add(new Article(document));
            }
            return results;
        }catch(MongoException e){
            return null;
        }
    }
}
