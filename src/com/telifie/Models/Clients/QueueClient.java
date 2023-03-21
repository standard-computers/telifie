package com.telifie.Models.Clients;

import com.telifie.Models.Article;
import com.telifie.Models.Domain;
import com.telifie.Models.Actions.Parser;
import org.bson.Document;

public class QueueClient extends Client {

    public QueueClient(Domain domain) {

        super(domain);
        super.collection = "queue";
    }

    public Article add(String uri){

        Article parsed = Parser.engines.parse(uri);
        if(this.insertOne(Document.parse(parsed.toString()))){

           return parsed;
        }
        return null;
    }
}
