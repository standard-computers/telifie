package com.telifie.Models.Utilities;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.telifie.Models.Domain;
import com.telifie.Models.User;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;

public class Configuration implements Serializable {

    private ArrayList<String> ipList; //List of IP to externally connect
    private ArrayList<String> ipAccess; //List of IP to allowed to access server
    private String mongoURI; //JSON of database configuration

    private User user;
    public static MongoClient mongoClient;
    protected Domain domain;

    public String getURI() {
        return mongoURI;
    }

    public void setMongoURI(String mongoURI) {
        this.mongoURI = mongoURI;
    }

    public String getMongoURI() {
        return mongoURI;
    }

    public void startMongo(){
        mongoClient = MongoClients.create(getMongoURI());
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public MongoClient getClient(){
        return mongoClient;
    }

    public Domain getDomain() {
        return domain;
    }

    public void setDomain(Domain domain) {
        this.domain = domain;
    }

    public boolean save(String systemDir){
        String dir = systemDir + "/telifie.configuration";
        serialized(dir, this);
        return true;
    }

    public static void serialized(String name, Serializable object){
        try {
            ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(name));
            out.writeObject(object);
            out.flush();
            out.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
