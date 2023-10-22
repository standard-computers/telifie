package com.telifie.Models.Clients;

import com.telifie.Models.Article;
import com.telifie.Models.Collection;
import com.telifie.Models.Utilities.Session;
import org.bson.Document;
import java.util.ArrayList;
import java.util.Arrays;

public class CollectionsClient extends Client {

    public CollectionsClient(Session session){
        super(session);
        super.collection = "collections";
    }

    public Collection withArticles(String id){
        Collection collection = new Collection(this.findOne(new Document("id", id)));
        ArrayList<Article> articles = new ArrayList<>();
        if(!collection.getDomain().equals("telifie")){
            session.setDomain(collection.getDomain());
        }
        ArticlesClient articlesClient = new ArticlesClient(session);
        if(collection.getArticles() != null || !collection.getArticles().isEmpty()){
            collection.getArticles().forEach(articleId -> articles.add(articlesClient.withId(articleId)));
            collection.setDetailedList(articles);
        }
        return collection;
    }

    public Collection get(String id){
        return new Collection(this.findOne(new Document("id", id)));
    }

    public ArrayList<Collection> forUser(String userId){
        ArrayList<Document> groups = this.find(new Document("user", userId));
        ArrayList<Collection> found = new ArrayList<>();
        groups.forEach(g -> found.add(new Collection(g)));
        return found;
    }

    public Collection create(Collection collection){
        collection.setUser(session.getUser());
        if(super.insertOne( Document.parse(collection.toString()) )){
            return collection;
        }
        return null;
    }

    public Collection create(String name){
        if(this.exists(new Document("$and", Arrays.asList(new Document("user", session.getUser()), new Document("name", name))))){
            return null;
        }
        Collection collection = new Collection(name);
        if(super.insertOne( Document.parse(collection.toString()) )){
            return collection;
        }
        return null;
    }

    public boolean save(Collection collection, Article article){
        return this.updateOne(new Document("$and", Arrays.asList(new Document("user", session.getUser()), new Document("id", collection.getId()))), new Document("$push", new Document("articles", article.getId())));
    }

    public boolean unsave(Collection collection, Article article){
        return this.updateOne(new Document("$and", Arrays.asList(new Document("user", session.getUser()), new Document("id", collection.getId()))), new Document("$pull", new Document("articles", article.getId())));
    }

    public boolean delete(Collection collection){
        return this.deleteOne(new Document("$and", Arrays.asList(new Document("user", session.getUser()), new Document("id", collection.getId()))));
    }

    public boolean update(Collection collection, Document update){
        return super.updateOne(new Document("id", collection.getId()), new Document("$set", update));
    }
}
