package com.telifie.Models.Clients;

import com.telifie.Models.Article;
import com.telifie.Models.Shortcut;
import com.telifie.Models.Utilities.Session;
import org.bson.Document;
import java.util.ArrayList;
import java.util.Arrays;

public class Shortcuts extends Client {

    public Shortcuts(Session session){
        super(session);
        super.collection = "shortcuts";
    }

    public Shortcut withArticles(String id){
        Shortcut shortcut = new Shortcut(this.findOne(new Document("id", id)));
        ArrayList<Article> articles = new ArrayList<>();
        Articles articlesClient = new Articles(session);
        if(shortcut.getArticles() != null || !shortcut.getArticles().isEmpty()){
            //TODO check for '/' in article to see if in another domain
            shortcut.getArticles().forEach(articleId -> articles.add(articlesClient.withId(articleId)));
            shortcut.setDetailedList(articles);
        }
        return shortcut;
    }

    public Shortcut get(String id){
        return new Shortcut(this.findOne(new Document("id", id)));
    }

    public ArrayList<Shortcut> forUser(String userId){
        return this.find(new Document("user", userId), new Document("name", 1)).map(Shortcut::new).into(new ArrayList<>());
    }

    public Shortcut create(Shortcut shortcut){
        if(super.insertOne( Document.parse(shortcut.toString()) )){
            return shortcut;
        }
        return null;
    }

    public Shortcut create(String name){
        if(this.exists(new Document("$and", Arrays.asList(new Document("user", session.user), new Document("name", name))))){
            return null;
        }
        Shortcut shortcut = new Shortcut(name);
        if(super.insertOne( Document.parse(shortcut.toString()) )){
            return shortcut;
        }
        return null;
    }

    public boolean save(Shortcut shortcut, Article article){
        return this.updateOne(new Document("$and", Arrays.asList(new Document("user", session.user), new Document("id", shortcut.getId()))), new Document("$push", new Document("articles", article.getId())));
    }

    public boolean unsave(Shortcut shortcut, Article article){
        return this.updateOne(new Document("$and", Arrays.asList(new Document("user", session.user), new Document("id", shortcut.getId()))), new Document("$pull", new Document("articles", article.getId())));
    }

    public boolean delete(Shortcut shortcut){
        return this.deleteOne(new Document("$and", Arrays.asList(new Document("user", session.user), new Document("id", shortcut.getId()))));
    }

    public boolean update(Shortcut shortcut, Document update){
        return super.updateOne(new Document("id", shortcut.getId()), new Document("$set", update));
    }
}