package com.telifie.Models.Clients;

import com.telifie.Models.Article;
import com.telifie.Models.Actions.Parser;
import com.telifie.Models.Utilities.Configuration;
import org.bson.Document;

import java.util.ArrayList;

public class QueueClient extends Client {

    public QueueClient(Configuration config) {
        super(config);
        super.collection = "queue";
    }

    public Article add(String uri){
        Article parsed = Parser.engines.parse(uri);
        if(this.insertOne(Document.parse(parsed.toString()))){
           return parsed;
        }
        return null;
    }

    public boolean update(Article article, Article newArticle){
        return super.updateOne(new Document("id", article.getId()), new Document("$set", Document.parse(newArticle.toString())));
    }

    public boolean create(Article article){
        return super.insertOne(Document.parse(article.toString()));
    }

    public boolean delete(String articleId) {
        return super.deleteOne(new Document("id", articleId));
    }

    public Article get(String articleId){
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
}
