package com.telifie.Models.Clients;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.telifie.Models.Utilities.Console;
import com.telifie.Models.Utilities.Package;
import com.telifie.Models.Utilities.Session;
import org.bson.Document;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Packages extends Client {

    private static ArrayList<Package> packages = new ArrayList<>();

    public Packages(Session session){
        super(session);
        super.collection = "packages";
        Console.log("LOADING PACKAGES...");
        check();
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        Runnable task = () -> {
            Console.log("CHECKING PACKAGE HEALTH...");
            check();
        };
        scheduler.scheduleAtFixedRate(task, 0, 300, TimeUnit.SECONDS);
    }

    private void check(){
        packages.removeAll(packages);
        super.find(new Document(), new Document("name", 1)).into(new ArrayList<>()).forEach(p -> this.packages.add(new ObjectMapper().convertValue(p, Package.class)));
        Console.log(packages.size() + " PACKAGES LOADED");
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