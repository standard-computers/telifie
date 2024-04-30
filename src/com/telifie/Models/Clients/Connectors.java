package com.telifie.Models.Clients;

import com.telifie.Models.Connector;
import com.telifie.Models.Utilities.Session;
import org.bson.Document;
import java.util.ArrayList;
import java.util.Arrays;

public class Connectors extends Client{

    public Connectors(Session session){
        super(session);
        super.collection = "connectors";
    }

    public boolean create(Connector connector){
        return super.insertOne(Document.parse(connector.toString()));
    }

    public boolean delete(Connector connector){
        return super.deleteOne(new Document("user", connector.getUser()).append("id", connector.getId()));
    }

    public boolean exists(Connector connector){
        return super.exists(new Document("user", connector.getUser()).append("id", connector.getId()));
    }

    public Connector getConnector(String id){
        return new Connector(super.findOne(new Document("$and", Arrays.asList(new Document("user", session.user), new Document("id", id)))));
    }

    public ArrayList<Connector> mine(){
        return super.find(new Document("user", session.user)).map(Connector::new).into(new ArrayList<>());
    }
}
