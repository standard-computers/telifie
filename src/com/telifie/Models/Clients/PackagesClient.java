package com.telifie.Models.Clients;

import com.telifie.Models.Utilities.Session;
import com.telifie.Models.Utilities.Package;
import org.bson.Document;
import java.util.ArrayList;
import java.util.Arrays;

public class PackagesClient extends Client {

    public PackagesClient(Session session){
        super(session);
        super.collection = "packages";
    }

    public Package get(String id){
        return new Package(this.findOne(new Document("id", id)));
    }

    public ArrayList<Package> get(boolean isPublic){
        ArrayList<Package> packages = new ArrayList<>();
        this.find(new Document("public", isPublic)).forEach(p -> packages.add(new Package(p)));
        return packages;
    }

    public ArrayList<Package> get(){
        ArrayList<Package> packages = new ArrayList<>();
        this.find(new Document()).forEach(p -> packages.add(new Package(p)));
        return packages;
    }

    public Package get(String id, int version){
        return new Package(this.findOne(new Document("$and", Arrays.asList(new Document("id", id), new Document("version", version)))));
    }

    public int versions(String id){
        return this.count(new Document("id", id));
    }

    public boolean delete(String id, int version){
        return this.deleteOne(new Document("$and", Arrays.asList(new Document("id", id), new Document("version", version))));
    }

    public boolean create(Package p){
        return this.insertOne(Document.parse(p.toString()));
    }
}
