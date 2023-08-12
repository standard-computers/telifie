package com.telifie.Models.Clients;

import com.telifie.Models.Article;
import com.telifie.Models.Collection;
import com.telifie.Models.Domain;
import com.telifie.Models.Utilities.Configuration;
import org.bson.Document;
import java.util.ArrayList;
import java.util.Arrays;

public class CollectionsClient extends Client {

    public CollectionsClient(Configuration config){
        super(config);
        super.collection = "collections";
    }

    public Collection get(String userId, String id){
        Collection collection = new Collection(this.findOne(new Document("id", id)));
        if(collection.getPermissions() == 0){
            if(!collection.getUser().equals(userId)){
                return null;
            }
        }
        if(collection.getDomain().equals(userId)){
            Domain dm = new Domain();
            dm.setId(userId);
            this.config = new Configuration();
            this.config.setDomain(dm);
        }
        ArrayList<Article> articles = new ArrayList<>();
        ArticlesClient articlesClient = new ArticlesClient(this.getConfig());
        if(collection.getArticles() != null || collection.getArticles().size() > 0){
            for (String articleId : collection.getArticles()) {
                articles.add(articlesClient.withId(articleId));
            }
            collection.setDetailedList(articles);
        }
        return collection;
    }

    public ArrayList<Collection> forUser(String userId){
        ArrayList<Document> groups = this.find(new Document("user", userId));
        ArrayList<Collection> found = new ArrayList<>();
        groups.forEach(g -> found.add(new Collection(g)));
        return found;
    }

    public Collection create(Collection collection){
        collection.setUser(config.getAuthentication().getUser());
        if(super.insertOne( Document.parse(collection.toString()) )){
            return collection;
        }
        return null;
    }

    public Collection create(String name){
        if(this.exists(
                new Document("$and", Arrays.asList(
                        new Document("user", config.getAuthentication().getUser()),
                        new Document("name", name)
                )
            )
        )){
            return null;
        }
        Collection collection = new Collection(name);
        if(super.insertOne( Document.parse(collection.toString()) )){
            return collection;
        }
        return null;
    }

    public boolean save(Collection collection, Article article){
        return this.updateOne(
            new Document("$and",
                Arrays.asList(
                    new Document("user", config.getAuthentication().getUser()),
                    new Document("id", collection.getId())
                )
            ),
            new Document("$push", new Document("articles", article.getId()))
        );
    }

    public boolean unsave(Collection collection, Article article){
        return this.updateOne(
            new Document("$and",
                Arrays.asList(
                    new Document("user", config.getAuthentication().getUser()),
                    new Document("id", collection.getId())
                )
            ),
            new Document("$pull", new Document("articles", article.getId()))
        );
    }

    public boolean delete(Collection collection){
        return this.deleteOne(
            new Document("$and",
                Arrays.asList(
                    new Document("user", config.getAuthentication().getUser()),
                    new Document("id", collection.getId())
                )
            )
        );
    }

    public boolean update(Collection collection, Document update){
        return super.updateOne(new Document("id", collection.getId()), new Document("$set", update));
    }
}
