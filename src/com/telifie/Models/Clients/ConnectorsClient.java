package com.telifie.Models.Clients;

import com.telifie.Models.Connector;
import com.telifie.Models.Utilities.Configuration;
import org.bson.Document;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * This class is for handling User's individual connectors.
 * Connector authentication information for access
 * is remotely stored on the front end.
 *<p></p>
 * Connectors only process requests by the already authenticated by the front-end.
 */
public class ConnectorsClient extends Client{

    public ConnectorsClient(Configuration config){
        super(config);
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
        return new Connector(
                super.findOne(new Document("$and", Arrays.asList(
                        new Document("user", config.getAuthentication().getUser()),
                        new Document("id", id)
                ))
            )
        );
    }

    public ArrayList<Connector> mine(){
        ArrayList<Connector> connectors = new ArrayList<>();
        for(Document document : super.find(new Document("user", config.getAuthentication().getUser()))){
            connectors.add(new Connector(document));
        }
        return connectors;
    }
}
