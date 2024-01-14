package com.telifie.Models.Clients;

import com.mongodb.MongoException;
import com.mongodb.client.*;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.result.UpdateResult;
import com.telifie.Models.Utilities.Configuration;
import com.telifie.Models.Utilities.Session;
import org.bson.Document;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Client {

    protected static MongoClient mc;
    protected String collection;
    protected Session session;

    protected Client(Session session){
        this.session = session;
        mc = Configuration.mongoClient;
    }

    protected List<Document> find(Document filter){
        try {
            MongoCollection<Document> c = mc.getDatabase("telifie").getCollection(this.collection);
            return c.find(filter).limit(300000).into(new ArrayList<>());
        }catch(MongoException e){
            return null;
        }
    }

    protected List<Document> findWithProjection(Document filter, Document projection){
        try {
            MongoCollection<Document> c = mc.getDatabase("telifie").getCollection(this.collection);
            return c.find(filter).projection(projection).limit(420).into(new ArrayList<>());
        }catch(MongoException e){
            return null;
        }
    }

    protected Document findOne(Document filter){
        try {
            return mc.getDatabase("telifie").getCollection(this.collection).find(filter).first();
        }catch(MongoException e){
            return null;
        }
    }

    protected int count(Document filter){
        try {
            MongoCollection<Document> c = mc.getDatabase("telifie").getCollection(this.collection);
            return (int) c.countDocuments(filter);
        }catch(MongoException e){
            return -1;
        }
    }

    protected boolean updateOne(Document filter, Document update){
        try {
            MongoCollection<Document> c = mc.getDatabase("telifie").getCollection(this.collection);
            UpdateResult result = c.updateOne(filter, update);
            return result.getModifiedCount() > 0;
        }catch(MongoException e){
            return false;
        }
    }

    protected boolean updateOne(Document filter, Document update, UpdateOptions options){
        try {
            MongoCollection<Document> c = mc.getDatabase("telifie").getCollection(this.collection);
            UpdateResult result = c.updateOne(filter, update, options);
            return result.getModifiedCount() > 0;
        }catch(MongoException e){
            return false;
        }
    }

    protected boolean insertOne(Document document){
        try {
            MongoCollection<Document> c = mc.getDatabase("telifie").getCollection(this.collection);
            c.insertOne(document);
            return true;
        }catch(MongoException e){
            return false;
        }
    }

    protected boolean insertMany(ArrayList<Document> documents){
        try {
            MongoCollection<Document> c = mc.getDatabase("telifie").getCollection(this.collection);
            c.insertMany(documents);
            return true;
        }catch(MongoException e){
            return false;
        }
    }

    protected List<Document> aggregate(Document filter){
        try {
            MongoCollection<Document> c = mc.getDatabase("telifie").getCollection(this.collection);
            return c.aggregate(Arrays.asList(filter)).into(new ArrayList<>());
        }catch(MongoException e){
            return null;
        }
    }

    protected int count(){
        try {
            MongoCollection<Document> c = mc.getDatabase("telifie").getCollection(this.collection);
            return (int) c.countDocuments();
        }catch(MongoException e){
            return -1;
        }
    }

    protected boolean exists(Document filter){
        Document data = this.findOne(filter);
        return data != null;
    }

    protected boolean deleteOne(Document filter){
        try {
            MongoCollection<Document> c = mc.getDatabase("telifie").getCollection(this.collection);
            c.deleteOne(filter);
            return true;
        }catch(MongoException e){
            return false;
        }
    }
}