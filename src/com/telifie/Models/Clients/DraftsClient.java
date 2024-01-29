package com.telifie.Models.Clients;

import com.telifie.Models.Article;
import com.telifie.Models.Utilities.Session;
import org.bson.Document;
import java.util.ArrayList;

public class DraftsClient extends Client {

    public DraftsClient(Session session){
        super(session);
        super.collection = "drafts";
    }

    public boolean update(Article article, Article newArticle){
        return super.updateOne(new Document("id", article.getId()), new Document("$set", Document.parse(newArticle.toString())));
    }

    public boolean create(Article article){
        if(article.getOwner() == null || article.getOwner().isEmpty()){
            article.setOwner(session.user);
        }
        return super.insertOne(Document.parse(article.toString()));
    }

    public Article withId(String articleId) {
        return new Article(this.findOne(new Document("id", articleId)));
    }

    public ArrayList<Article> get(Document filter){
        return this.find(filter).map(Article::new).into(new ArrayList<>());
    }

    public ArrayList<Article> forUser(){
        return this.find(new Document("owner", session.user)).map(Article::new).into(new ArrayList<>());
    }
}