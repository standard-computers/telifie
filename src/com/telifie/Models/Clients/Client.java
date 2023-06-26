package com.telifie.Models.Clients;

import com.mongodb.MongoException;
import com.mongodb.client.*;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.result.UpdateResult;
import com.telifie.Models.Utilities.Configuration;
import org.bson.Document;
import java.util.ArrayList;

public class Client {

    private final String mongoUri;
    protected String collection;
    protected Configuration config;

    protected Client(Configuration config){
        this.config = config;
        this.mongoUri = config.getDomain().getUri();
    }

    public Configuration getConfig() {
        return config;
    }

    protected ArrayList<Document> find(Document filter){
        try(MongoClient mongoClient = MongoClients.create(this.mongoUri)){
            MongoDatabase database = mongoClient.getDatabase(config.getDomain().getAlt());
            MongoCollection<Document> collection = database.getCollection(this.collection);
            FindIterable<Document> iter = collection.find(filter).limit(500);
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
        try(MongoClient mongoClient = MongoClients.create(this.mongoUri)){
            MongoDatabase database = mongoClient.getDatabase(config.getDomain().getAlt());
            MongoCollection<Document> collection = database.getCollection(this.collection);
            return collection.find(filter).first();
        }catch(MongoException e){
            return null;
        }
    }

    protected boolean updateOne(Document filter, Document update){
        try(MongoClient mongoClient = MongoClients.create(this.mongoUri)){
            MongoDatabase database = mongoClient.getDatabase(config.getDomain().getAlt());
            MongoCollection<Document> collection = database.getCollection(this.collection);
            UpdateResult result = collection.updateOne(filter, update);
            return result.getModifiedCount() > 0;
        }catch(MongoException e){
            return false;
        }
    }

    protected boolean updateOne(Document filter, Document update, UpdateOptions options){
        try(MongoClient mongoClient = MongoClients.create(this.mongoUri)){
            MongoDatabase database = mongoClient.getDatabase(config.getDomain().getAlt());
            MongoCollection<Document> collection = database.getCollection(this.collection);
            UpdateResult result = collection.updateOne(filter, update, options);
            return result.getModifiedCount() > 0;
        }catch(MongoException e){
            return false;
        }
    }

    protected boolean insertOne(Document document){
        try(MongoClient mongoClient = MongoClients.create(this.mongoUri)){
            MongoDatabase database = mongoClient.getDatabase(config.getDomain().getAlt());
            MongoCollection<Document> collection = database.getCollection(this.collection);
            collection.insertOne(document);
            return true;
        }catch(MongoException e){
            return false;
        }
    }

    protected boolean exists(Document filter){
        Document data = this.findOne(filter);
        return data != null;
    }

    protected boolean deleteOne(Document filter){
        try(MongoClient mongoClient = MongoClients.create(this.mongoUri)){
            MongoDatabase database = mongoClient.getDatabase(config.getDomain().getAlt());
            MongoCollection<Document> collection = database.getCollection(this.collection);
            collection.deleteOne(filter);
            return true;
        }catch(MongoException e){
            return false;
        }
    }
}