package com.telifie.Models.Clients;

import com.telifie.Models.Utilities.Session;
import com.telifie.Models.Utilities.Package;
import org.bson.Document;
import java.util.Arrays;

public class PackagesClient extends Client {

    public PackagesClient(Session session){
        super(session);
        super.collection = "packages";
    }

    public Package get(String id){
        return new Package(this.findOne(new Document("id", id)));
    }

    public Package get(String name, int version){
        return new Package(this.findOne(new Document("$and", Arrays.asList(new Document("name", name), new Document("version", version)))));
    }

    public boolean delete(String name, int version){
        return this.deleteOne(new Document("$and", Arrays.asList(new Document("name", name), new Document("version", version))));
    }

    public boolean create(Package p){
        return this.insertOne(Document.parse(p.toString()));
    }
}
