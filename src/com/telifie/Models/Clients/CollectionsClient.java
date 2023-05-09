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

        if(collection.getDomain().equals(userId)){ //Personal Domain
            Domain dm = new Domain(this.config.getDomain().getUri());
            dm.setAlt(userId);
            this.config = new Configuration();
            this.config.setDomain(dm);
        }
        ArrayList<Article> articles = new ArrayList<Article>();
        ArticlesClient articlesClient = new ArticlesClient(super.getConfig());
        if(collection.getArticles() != null){
            for (String articleId : collection.getArticles()) {
                articles.add(articlesClient.get(articleId));
            }
            collection.setDetailedList(articles);
            return collection;
        }
        return null;
    }

    public ArrayList<Collection> forUser(String userId){
        ArrayList<Document> groups = this.find(new Document("user", userId));
        ArrayList<Collection> found = new ArrayList<>();
        for (Document doc : groups) {
            found.add(new Collection(doc));
        }
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

    public boolean save(String groupId, String articleId){

        return this.updateOne(
            new Document("$and",
                Arrays.asList(
                    new Document("user", config.getAuthentication().getUser()),
                    new Document("id", groupId)
                )
            ),
            new Document("$push", new Document("articles", articleId))
        );
    }

    public boolean unsave(String groupId, String articleId){

        return this.updateOne(
            new Document("$and",
                Arrays.asList(
                    new Document("user", config.getAuthentication().getUser()),
                    new Document("id", groupId)
                )
            ),
            new Document("$pull", new Document("articles", articleId))
        );
    }

    public boolean delete(String groupId){
        return this.deleteOne(
            new Document("$and",
                Arrays.asList(
                    new Document("user", config.getAuthentication().getUser()),
                    new Document("id", groupId)
                )
            )
        );
    }

    public boolean update(String groupId, Document update){
        return super.updateOne(new Document("id", groupId), new Document("$set", update));
    }
}
