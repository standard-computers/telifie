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
            article.setOwner(session.getUser());
        }
        return super.insertOne(Document.parse(article.toString()));
    }

    public boolean createMany(ArrayList<Article> articles){
        ArrayList<Document> documents = new ArrayList<>();
        articles.forEach(a -> documents.add(Document.parse(a.toString())));
        return super.insertMany(documents);
    }

    public Article withId(String articleId) {
        return new Article(this.findOne(new Document("id", articleId)));
    }

    public ArrayList<Article> get(Document filter){
        ArrayList<Article> articles = new ArrayList<>();
        this.find(filter).forEach(f -> articles.add(new Article(f)));
        return articles;
    }

    public ArrayList<Article> forUser(){
        ArrayList<Article> articles = new ArrayList<>();
        this.find(new Document("owner", session.getUser())).forEach(f -> articles.add(new Article(f)));
        return articles;
    }
}
