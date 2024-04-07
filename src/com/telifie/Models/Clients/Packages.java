package com.telifie.Models.Clients;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.telifie.Models.Utilities.Log;
import com.telifie.Models.Utilities.Package;
import com.telifie.Models.Utilities.Session;
import org.bson.Document;
import java.util.ArrayList;
import java.util.List;

public class Packages extends Client {

    private static ArrayList<Package> packages = new ArrayList<>();

    public Packages(Session session){
        super(session);
        super.collection = "packages";
        Log.console("LOADING PACKAGES...");
        packages.removeAll(packages);
        super.find(new Document(), new Document("name", 1)).into(new ArrayList<>()).forEach(p -> this.packages.add(new ObjectMapper().convertValue(p, Package.class)));
        Log.console(packages.size() + " PACKAGES LOADED");
    }

    public static Package get(String id){
        for (Package p : packages){
            if(p.getId().equals(id)){
                return p;
            }
        }
        return null;
    }

    public static List<Package> getPublic(){
        ArrayList<Package> ps = new ArrayList<>();
        packages.forEach(p -> {
            if(p.getPublic()){
                ps.add(p);
            }
        });
        return ps;
    }
}