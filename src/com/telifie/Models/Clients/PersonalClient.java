package com.telifie.Models.Clients;

import com.telifie.Models.Utilities.Session;
import org.bson.Document;

public class PersonalClient extends Client {

    public PersonalClient(Session session){
        super(session);
        super.collection = "personal";
    }

    public Document next(){
        Document n = super.next(3);
        super.deleteOne(new Document("link", n.getString("link")));
        return n;
    }

    public boolean hasNext(){
        return super.hasNext();
    }
}