package com.telifie.Models.Clients;

import com.telifie.Models.Article;
import com.telifie.Models.Domain;
import com.telifie.Models.Parser;
import org.bson.Document;

public class QueueClient extends Client {

    public QueueClient(Domain domain) {

        super(domain);
        super.collection = "queue";
    }

    public Article add(String uri){

        Article parsed = Parser.engine.parse(uri);
        if(this.insertOne(Document.parse(parsed.toString()))){

           return parsed;
        }else{

            return null;
        }
    }
}
