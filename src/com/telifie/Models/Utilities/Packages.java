package com.telifie.Models.Utilities;

import com.telifie.Models.Clients.PackagesClient;

import java.util.ArrayList;

public class Packages {

    private static ArrayList<Package> packages;

    public Packages(Session session){
        packages = new PackagesClient(session).get();
        Log.message(packages.size() + " PACKAGES LOADED");
    }

    public static Package get(String id){
        for (Package p : packages){
            if(p.getId().equals(id)){
                return p;
            }
        }
        return null;
    }
}
