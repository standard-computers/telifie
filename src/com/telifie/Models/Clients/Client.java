package com.telifie.Models.Clients;

import com.mongodb.MongoException;
import com.mongodb.client.*;
import com.mongodb.client.result.UpdateResult;
import com.telifie.Models.Utilities.Configuration;
import com.telifie.Models.Utilities.Session;
import org.bson.Document;
import java.util.ArrayList;
import java.util.Collections;
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
            return mc.getDatabase(session.getDomain()).getCollection(this.collection).find(filter);
        }catch(MongoException e){
            return null;
        }
    }

    protected FindIterable<Document> find(Document filter, Document sort){
        try {
            return mc.getDatabase(session.getDomain()).getCollection(this.collection).find(filter).sort(sort);
        }catch(MongoException e){
            return null;
        }
    }

    protected FindIterable<Document> findWithProjection(Document filter, Document projection){
        try {
            return mc.getDatabase(session.getDomain()).getCollection(this.collection).find(filter).projection(projection);
        }catch(MongoException e){
            return null;
        }
    }

    protected Document findOne(Document filter){
        try {
            return mc.getDatabase(session.getDomain()).getCollection(this.collection).find(filter).first();
        }catch(MongoException e){
            return null;
        }
    }

    protected Document next(int skip) {
        MongoCursor<Document> cursor = mc.getDatabase(session.getDomain()).getCollection(this.collection).find().skip(skip).limit(1).iterator();
        if (cursor.hasNext()) {
            return cursor.next();
        } else {
            return null;
        }
    }

    protected boolean hasNext() {
        return mc.getDatabase(session.getDomain()).getCollection(this.collection).find().limit(1).iterator().hasNext();
    }

    protected boolean updateOne(Document filter, Document update){
        try {
            UpdateResult result = mc.getDatabase(session.getDomain()).getCollection(this.collection).updateOne(filter, update);
            return result.getModifiedCount() > 0;
        }catch(MongoException e){
            return false;
        }
    }

    protected boolean insertOne(Document document){
        try {
            mc.getDatabase(session.getDomain()).getCollection(this.collection).insertOne(document);
            return true;
        }catch(MongoException e){
            return false;
        }
    }

    protected List<Document> aggregate(Document filter){
        try {
            return mc.getDatabase(session.getDomain()).getCollection(this.collection).aggregate(Collections.singletonList(filter)).into(new ArrayList<>());
        }catch(MongoException e){
            return null;
        }
    }

    protected int count(){
        try {
            return (int) mc.getDatabase(session.getDomain()).getCollection(this.collection).countDocuments();
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
            mc.getDatabase(session.getDomain()).getCollection(this.collection).deleteOne(filter);
            return true;
        }catch(MongoException e){
            return false;
        }
    }
}