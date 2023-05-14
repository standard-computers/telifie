package com.telifie.Models.Clients;

import com.telifie.Models.Article;
import com.telifie.Models.Utilities.Configuration;
import org.bson.Document;

import java.util.ArrayList;

public class ArchiveClient extends Client {

    public ArchiveClient(Configuration config){
        super(config);
        super.collection = "archive";
    }

    public boolean archive(Article article){
        ArticlesClient articles = new ArticlesClient(config);
        articles.delete(article);
        return super.insertOne(Document.parse(article.toString()));
    }

    public boolean unarchive(Article article){
        ArticlesClient articles = new ArticlesClient(config);
        articles.create(article);
        return super.deleteOne(new Document("id", article.getId()));
    }

    public Article withId(String articleId){
        return new Article(this.findOne(new Document("id", articleId)));
    }

    public ArrayList<Article> get(){
        ArrayList<Document> found = this.find(new Document());
        ArrayList<Article> articles = new ArrayList<>();
        for(Document doc : found){
            articles.add(new Article(doc));
        }
        return articles;
    }
}
