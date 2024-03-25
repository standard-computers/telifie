package com.telifie.Models.Clients;

import com.telifie.Models.Article;
import com.telifie.Models.Utilities.Session;
import org.bson.Document;
import java.util.ArrayList;

public class PersonalClient extends Client {

    public PersonalClient(Session session){
        super(session);
        super.collection = "personal";
    }

    public boolean create(Article article){
        if(article.getOwner() == null || article.getOwner().isEmpty()){
            article.setOwner(session.user);
        }
        return super.insertOne(Document.parse(article.toString()));
    }

    public Document next(){
        Document n = super.next(10);
        super.deleteOne(new Document("link", n.getString("link")));
        return n;
    }

    public boolean hasNext(){
        return super.hasNext();
    }

    public ArrayList<Article> get(Document filter){
        return this.find(filter).map(Article::new).into(new ArrayList<>());
    }
}