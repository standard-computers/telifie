package com.telifie.Models.Clients;

import com.mongodb.BasicDBObject;
import com.mongodb.MongoException;
import com.mongodb.client.*;
import com.telifie.Models.Domain;
import com.telifie.Models.Utilities.Parameters;
import com.telifie.Models.Article;
import com.telifie.Models.Utilities.Configuration;
import org.bson.Document;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.TreeMap;

public class ArticlesClient extends Client {

    public ArticlesClient(Configuration config){
        super(config);
        if(config.getDomain().getId() != null && !config.getDomain().getId().equals("telifie")){
            super.collection = "domain-articles";
        }else{
            super.collection = "articles";
        }
    }

    public boolean update(Article article, Article newArticle){
        return super.updateOne(new Document("id", article.getId()), new Document("$set", Document.parse(newArticle.toString())));
    }

    public boolean create(Article article){
        if(article.getLink() == null || article.getLink().equals("")){
            return super.insertOne(Document.parse(article.toString()));
        }else if(this.withLink(article.getLink()) == null){
            return super.insertOne(Document.parse(article.toString()));
        }else{
            return false;
        }
    }

    public ArrayList<Article> linked(){
        return this.get(
            new Document("$or",
                Arrays.asList(
                        new Document("source.url", new Document("$exists", true)),
                        new Document("link", new Document("$exists", true))
                )
            )
        );
    }

    public boolean createMany(ArrayList<Article> articles){
        ArrayList<Document> documents = new ArrayList<>();
        for(Article article : articles){
            documents.add(Document.parse(article.toString()));
        }
        return super.insertMany(documents);
    }

    public Article withLink(String link){
        try{
            return new Article(this.findOne(new Document("link", link)));
        }catch (NullPointerException e){
            return null;
        }
    }

    public boolean verify(Article article){
        return this.updateOne(new Document("id", article.getId()), new Document("$set", new Document("verified", true)));
    }

    public Article withId(String articleId) {
        return new Article(this.findOne(new Document("id", articleId)));
    }

    public ArrayList<Article> get(Document filter){
        ArrayList<Document> found = this.find(filter);
        ArrayList<Article> articles = new ArrayList<>();
        for(Document doc : found){
            articles.add(new Article(doc));
        }
        return articles;
    }

    public ArrayList<Article> search(Parameters params, Document filter){
        try {
            MongoDatabase database = super.mc.getDatabase("telifie");
            MongoCollection<Document> collection = database.getCollection(super.collection);
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

    public boolean delete(Article article) {
        return super.deleteOne(new Document("id", article.getId()));
    }

    public ArrayList<Document> getIds(String q){
        return super.find(new Document("verified", false));
    }

    public boolean move(Article article, Domain domain){
        this.delete(article);
        Configuration c2 = new Configuration();
        c2.setDomain(domain);
        this.config = c2;
        return this.create(article);
    }

    public boolean duplicate(Article article, Domain domain){
        Configuration c2 = new Configuration();
        c2.setDomain(domain);
        this.config = c2;
        return this.create(article);
    }

    public boolean exists(String id){
        return super.exists(new Document("id", id));
    }

    public boolean existsWithSource(String source){
        return (super.findOne(new Document("source.url", source)) == null ? false : true);
    }

    public ArrayList<Document> withSource(String source){
        if(source.startsWith("http")){
            return super.find(new Document("source.url", source));
        }
        return super.find(new Document("source.name", source));
    }

    public Document stats() {
        Document groupFields = new Document("_id", "$description");
        groupFields.put("count", new Document("$sum", 1));
        Document groupStage = new Document("$group", groupFields);
        ArrayList<Document> iterable = super.aggregate(groupStage);
        Document stats = new Document();
        int total = super.count();
        stats.append("total", total);
        TreeMap<String, Document> sortedDescriptions = new TreeMap<>();
        for (Document document : iterable) {
            String description = document.getString("_id");
            int count = document.getInteger("count");
            if (description == null) {
                description = "Unclassified";
            }
            double percent = (double) count / total * 100; // Calculate the percentage
            Document descriptionStats = new Document();
            descriptionStats.append("count", count);
            descriptionStats.append("percent", percent);
            sortedDescriptions.put(description, descriptionStats);
        }
        Document descriptions = new Document();
        sortedDescriptions.forEach((key, value) -> descriptions.append(key, value));
        stats.append("descriptions", descriptions);
        return stats;
    }

    public boolean imageExists(String imageUrl){
        return exists(new Document("images.url", imageUrl));
    }

    public boolean archive(Article article){
        ArchiveClient archive = new ArchiveClient(config);
        return archive.archive(article);
    }
}
