package com.telifie.Models.Clients;

import com.telifie.Models.Article;
import com.telifie.Models.Domain;
import com.telifie.Models.Group;
import org.bson.Document;

import java.util.ArrayList;
import java.util.Arrays;

public class GroupsClient extends Client {

    public GroupsClient(Domain domain){

        super(domain);
        super.collection = "groups";

    }

    public Group get(String userId, String id){

        Group group = new Group(
            this.findOne(
                new Document("$and",
                    Arrays.asList(
                        new Document("user", userId),
                        new Document("id", id)
                    )
                )
            )
        );

        ArrayList<Article> articles = new ArrayList<Article>();
        ArticlesClient articlesClient = new ArticlesClient(this.domain);
        for (String articleId : group.getArticles()) {
            articles.add(articlesClient.get(articleId));
        }
        group.setDetailedList(articles);
        return group;

    }

    public ArrayList<Group> groupsForUser(String userId){

        ArrayList<Document> groups = this.find(new Document("user", userId));
        ArrayList<Group> found = new ArrayList<>();
        for (Document doc : groups) {
            found.add(new Group(doc));
        }

        return found;

    }

    public Group create(String userId, Group group){
        //TODO Install checks to ensure that proper information was provided by JSON
        group.setUser(userId);
        if(super.insertOne( Document.parse(group.toString()) )){

            return group;
        }else{

            return null;
        }
    }

    public Group create(String userId, String name){

        if(this.exists(
            new Document("$and",
                Arrays.asList(
                    new Document("user", userId ),
                    new Document("name", name )
                )
            )
        )){
            return null;
        }

        Group group = new Group(userId, name);
        if(super.insertOne( Document.parse(group.toString()) )){

            return group;
        }else{

            return null;
        }
    }

    public boolean save(String userId, String groupId, String articleId){

        return this.updateOne(
            new Document("$and",
                Arrays.asList(
                    new Document("user", userId),
                    new Document("id", groupId)
                )
            ),
            new Document("$push", new Document("articles", articleId))
        );
    }

    public boolean unsave(String userId, String groupId, String articleId){

        return this.updateOne(
            new Document("$and",
                Arrays.asList(
                    new Document("user", userId),
                    new Document("id", groupId)
                )
            ),
            new Document("$pull", new Document("articles", articleId))
        );
    }

    public boolean delete(String userId, String groupId){
        return this.deleteOne(
            new Document("$and",
                Arrays.asList(
                    new Document("user", userId),
                    new Document("name", groupId)
                )
            )
        );
    }

    public boolean update(String groupId, Document update){

        return super.updateOne(
            new Document("id", groupId),
            new Document("$set", update)
        );

    }

}
