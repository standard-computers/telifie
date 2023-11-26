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

public class Client {

    protected static MongoClient mc;
    protected String collection;
    protected Session session;

    protected Client(Session session){
        this.session = session;
        mc = Configuration.mongoClient;
    }

    protected ArrayList<Document> find(Document filter){
        try {
            MongoDatabase db = mc.getDatabase("telifie");
            MongoCollection<Document> c = db.getCollection(this.collection);
            FindIterable<Document> iter = c.find(filter);
            ArrayList<Document> documents = new ArrayList<>();
            for(Document doc : iter){
                documents.add(doc);
            }
            return documents;
        }catch(MongoException e){
            return null;
        }
    }

    protected ArrayList<Document> findWithProjection(Document filter, Document projection){
        try {
            MongoDatabase db = mc.getDatabase("telifie");
            MongoCollection<Document> c = db.getCollection(this.collection);
            FindIterable<Document> iter = c.find(filter).projection(projection);
            ArrayList<Document> documents = new ArrayList<>();
            for(Document doc : iter){
                documents.add(doc);
            }
            return documents;
        }catch(MongoException e){
            return null;
        }
    }

    protected Document findOne(Document filter){
        try {
            MongoDatabase db = mc.getDatabase("telifie");
            MongoCollection<Document> c = db.getCollection(this.collection);
            return c.find(filter).first();
        }catch(MongoException e){
            return null;
        }
    }

    protected int count(Document filter){
        try {
            MongoDatabase db = mc.getDatabase("telifie");
            MongoCollection<Document> c = db.getCollection(this.collection);
            return (int) c.countDocuments(filter);
        }catch(MongoException e){
            return -1;
        }
    }

    protected boolean updateOne(Document filter, Document update){
        try {
            MongoDatabase db = mc.getDatabase("telifie");
            MongoCollection<Document> c = db.getCollection(this.collection);
            UpdateResult result = c.updateOne(filter, update);
            return result.getModifiedCount() > 0;
        }catch(MongoException e){
            return false;
        }
    }

    protected boolean updateOne(Document filter, Document update, UpdateOptions options){
        try {
            MongoDatabase db = mc.getDatabase("telifie");
            MongoCollection<Document> c = db.getCollection(this.collection);
            UpdateResult result = c.updateOne(filter, update, options);
            return result.getModifiedCount() > 0;
        }catch(MongoException e){
            return false;
        }
    }

    protected boolean insertOne(Document document){
        try {
            MongoDatabase db = mc.getDatabase("telifie");
            MongoCollection<Document> c = db.getCollection(this.collection);
            c.insertOne(document);
            return true;
        }catch(MongoException e){
            return false;
        }
    }

    protected boolean insertMany(ArrayList<Document> documents){
        try {
            MongoDatabase db = mc.getDatabase("telifie");
            MongoCollection<Document> c = db.getCollection(this.collection);
            c.insertMany(documents);
            return true;
        }catch(MongoException e){
            return false;
        }
    }

    protected ArrayList<Document> aggregate(Document filter){
        try {
            MongoDatabase db = mc.getDatabase("telifie");
            MongoCollection<Document> c = db.getCollection(this.collection);
            AggregateIterable<Document> i = c.aggregate(Arrays.asList(filter));
            ArrayList<Document> documents = new ArrayList<>();
            for(Document doc : i){
                documents.add(doc);
            }
            return documents;
        }catch(MongoException e){
            return null;
        }
    }

    protected int count(){
        try {
            MongoDatabase db = mc.getDatabase("telifie");
            MongoCollection<Document> c = db.getCollection(this.collection);
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
            MongoDatabase db = mc.getDatabase("telifie");
            MongoCollection<Document> c = db.getCollection(this.collection);
            c.deleteOne(filter);
            return true;
        }catch(MongoException e){
            return false;
        }
    }
}