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

    protected FindIterable<Document> find(Document filter){
        try {
            return mc.getDatabase("telifie").getCollection(this.collection).find(filter);
        }catch(MongoException e){
            return null;
        }
    }

    protected FindIterable<Document> findWithProjection(Document filter, Document projection){
        try {
            return mc.getDatabase("telifie").getCollection(this.collection).find(filter).projection(projection);
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
            return (int) mc.getDatabase("telifie").getCollection(this.collection).countDocuments(filter);
        }catch(MongoException e){
            return -1;
        }
    }

    protected boolean updateOne(Document filter, Document update){
        try {
            UpdateResult result = mc.getDatabase("telifie").getCollection(this.collection).updateOne(filter, update);
            return result.getModifiedCount() > 0;
        }catch(MongoException e){
            return false;
        }
    }

    protected boolean updateOne(Document filter, Document update, UpdateOptions options){
        try {
            UpdateResult result = mc.getDatabase("telifie").getCollection(this.collection).updateOne(filter, update, options);
            return result.getModifiedCount() > 0;
        }catch(MongoException e){
            return false;
        }
    }

    protected boolean insertOne(Document document){
        try {
            mc.getDatabase("telifie").getCollection(this.collection).insertOne(document);
            return true;
        }catch(MongoException e){
            return false;
        }
    }

    protected List<Document> aggregate(Document filter){
        try {
            return mc.getDatabase("telifie").getCollection(this.collection).aggregate(Arrays.asList(filter)).into(new ArrayList<>());
        }catch(MongoException e){
            return null;
        }
    }

    protected int count(){
        try {
            return (int) mc.getDatabase("telifie").getCollection(this.collection).countDocuments();
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
            mc.getDatabase("telifie").getCollection(this.collection).deleteOne(filter);
            return true;
        }catch(MongoException e){
            return false;
        }
    }
}