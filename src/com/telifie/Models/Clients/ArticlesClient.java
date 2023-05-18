package com.telifie.Models.Clients;

import com.mongodb.BasicDBObject;
import com.mongodb.MongoException;
import com.mongodb.client.*;
import com.telifie.Models.Andromeda;
import com.telifie.Models.Domain;
import com.telifie.Models.Utilities.Parameters;
import com.telifie.Models.Article;
import com.telifie.Models.Utilities.Configuration;
import org.bson.Document;
import java.util.ArrayList;

public class ArticlesClient extends Client {

    public ArticlesClient(Configuration config){
        super(config);
        super.collection = "articles";
    }

    public boolean update(Article article, Article newArticle){
        return super.updateOne(new Document("id", article.getId()), new Document("$set", Document.parse(newArticle.toString())));
    }

    public boolean create(Article article){
        return super.insertOne(Document.parse(article.toString()));
    }

    public boolean verify(Article article){
        return this.updateOne(new Document("id", article.getId()), new Document("$set", new Document("verified", true)));
    }

    public Article withId(String articleId){
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

    public ArrayList<Article> search(Configuration config, Parameters params, Document filter){
        try(MongoClient mongoClient = MongoClients.create(config.getDomain().getUri())){
            MongoDatabase database = mongoClient.getDatabase(config.getDomain().getAlt());
            MongoCollection<Document> collection = database.getCollection("articles");
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

    public boolean move(Article article, Domain domain){
        this.delete(article);
        Configuration c2 = new Configuration();
        domain.setUri(config.getDomain().getUri());
        c2.setDomain(domain);
        this.config = c2;
        return this.create(article);
    }

    public boolean duplicate(Article article, Domain domain){
        Configuration c2 = new Configuration();
        domain.setUri(config.getDomain().getUri());
        c2.setDomain(domain);
        this.config = c2;
        return this.create(article);
    }

    public boolean exists(String id){
        return super.exists(new Document("id", id));
    }

    public String stats(){
        //TODO
        return "";
    }

    public boolean archive(Article article){
        ArchiveClient archive = new ArchiveClient(config);
        return archive.archive(article);
    }

}
