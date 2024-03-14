package com.telifie.Models.Clients;

import com.telifie.Models.Article;
import com.telifie.Models.Utilities.Session;
import org.bson.Document;
import java.util.ArrayList;

public class ArchiveClient extends Client {

    public ArchiveClient(Session session){
        super(session);
        super.collection = "archive";
    }

    public boolean archive(Article article){
        ArticlesClient articles = new ArticlesClient(session);
        articles.delete(article);
        return super.insertOne(Document.parse(article.toString()));
    }

    public boolean unarchive(Article article){
        ArticlesClient articles = new ArticlesClient(session);
        articles.create(article);
        return super.deleteOne(new Document("id", article.getId()));
    }

    public ArrayList<Article> get(){
        return this.find(new Document()).map(Article::new).into(new ArrayList<>());
    }
}
