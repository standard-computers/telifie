package com.telifie.Models.Utilities;

import com.mongodb.client.*;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import com.mongodb.client.model.geojson.Point;
import com.mongodb.client.model.geojson.Position;
import com.telifie.Models.Article;
import com.telifie.Models.Articles.Attribute;
import com.telifie.Models.Articles.Image;
import com.telifie.Models.Clients.ArticlesClient;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Scanner;

public class Console {

    public static void welcome() {
        System.out.println("\n");
        System.out.println("||===========================================================||");
        System.out.println("||                                                           ||");
        System.out.println("||  ,--------. ,------. ,--.    ,--. ,------. ,--. ,------.  ||");
        System.out.println("||  '--.  .--' |  .---' |  |    |  | |  .---' |  | |  .---'  ||");
        System.out.println("||     |  |    |  `--,  |  |    |  | |  `--,  |  | |  `--,   ||");
        System.out.println("||     |  |    |  `---. |  '--. |  | |  |`    |  | |  `---.  ||");
        System.out.println("||     `--'    `------' `-----' `--' `--'     `--' `------'  ||");
        System.out.println("||                                                           ||");
        System.out.println("||===========================================================||\n");
        String operatingSystem = System.getProperty("os.name");
        System.out.println("Operating System : " + operatingSystem);
        System.out.println("System Architecture : " + System.getProperty("os.arch"));
        Console.line();
    }

    public static void line(){
        System.out.println("--------------------------------------------------------------");
    }

    public static void message(String message){
        line();
        System.out.println(message);
        line();
    }

    public static void string(String message){
        System.out.println(message);
    }

    public static String in(){
        Scanner in = new Scanner(System.in);
        return in.nextLine();
    }

    public static String in(String prompt){
        System.out.print(prompt);
        Scanner in = new Scanner(System.in);
        return in.nextLine();
    }

    public static class command {

        public command(Configuration config){
            while(true){
                String cmd = Console.in("telifie -> ");
                if(cmd.equals("exit") || cmd.equals("logout") || cmd.equals("close")){
                    System.exit(1);
                }else if(cmd.equals("http")){
                    try {
                        new Http(config);
                    } catch (Exception e) {
                        Log.error("Failed to start HTTP server");
                        e.printStackTrace();
                    }
                }else if(cmd.equals("geocode")){
                    try {
                        MongoClient mongoClient = MongoClients.create(config.getURI());
                        MongoDatabase database = mongoClient.getDatabase("telifie");
                        MongoCollection<Document> collection = database.getCollection("articles");
                        FindIterable<Document> doc = collection.find(new Document("$and", Arrays.asList(new Document("attributes.key", "Longitude"), new Document("attributes.key", "Latitude"), new Document("location", new Document("$exists", false)))));
                        for(Document d : doc){
                            if (d != null) {
                                Article a = new Article(d);
                                String longitude = a.getAttribute("Longitude");
                                String latitude = a.getAttribute("Latitude");
                                if(longitude != null && latitude != null && !longitude.equals("null") && !latitude.equals("null")){
                                    double longitudeValue = Double.parseDouble(longitude);
                                    double latitudeValue = Double.parseDouble(latitude);
                                    Position position = new Position(longitudeValue, latitudeValue);
                                    Point point = new Point(position);
                                    Bson update = Updates.set("location", point);
                                    collection.updateOne(new Document("id", a.getId()), update);
                                    System.out.println(a.getTitle());
                                }
                            }
                        }
                        mongoClient.close();
                    } catch (Exception e) {
                        Log.error("Failed to start Geocode server");
                        e.printStackTrace();
                    }
                }else if(cmd.equals("clean")){

                }else if(cmd.equals("spimgs")){
                    MongoClient mongoClient = MongoClients.create(config.getURI());
                    MongoDatabase database = mongoClient.getDatabase("telifie");
                    MongoCollection<Document> collection = database.getCollection("articles");
                    FindIterable<Document> result = collection.find(Filters.exists("images", true));
                    ArticlesClient articles = new ArticlesClient(new Session("", "telifie"));
                    for (Document document : result) {
                        Article a = new Article(document);
                        System.out.println("Working -> " + a.getTitle() + " : " + a.getId());
                        ArrayList<ObjectId> imageIds = new ArrayList<>();
                        for(Image i : a.getImages()){
                            System.out.println("Image working -> "  + i.getUrl());
                            if(!i.getUrl().trim().startsWith("data:image/jpeg;base64") && !i.getUrl().trim().startsWith("data:image/png;base64")){
                                Article ia = new Article();
                                ia.setDescription("Image");
                                ia.setPriority(0.162);
                                ia.setTitle(a.getTitle());
                                ia.setLink(i.getUrl());
                                ia.addAttribute(new Attribute("Article", a.getId()));
                                if(a.getTags() != null){
                                    for(String s : a.getTags()){
                                        ia.addTag(s);
                                    }
                                }
                                Document in = Document.parse(ia.toString());
                                if(articles.withLink(ia.getLink()) == null){
                                    collection.insertOne(in);
                                    imageIds.add(in.getObjectId("_id"));
                                }
                            }
                            Document updateDoc = new Document("$set", new Document("imageref", imageIds));
                            Document removeImages = new Document("$unset", new Document("images", ""));
                            collection.updateOne(Filters.eq("id", a.getId()), updateDoc);
                            collection.updateOne(Filters.eq("id", a.getId()), removeImages);
                        }
                    }
                }else if(cmd.equals("d")){
                    MongoClient mongoClient = MongoClients.create(config.getURI());
                    MongoDatabase database = mongoClient.getDatabase("telifie");
                    MongoCollection<Document> collection = database.getCollection("articles");
                    FindIterable<Document> definitions = collection.find(new Document("$and", Arrays.asList(new Document("description","Definition"), new Document("verified", false))));
                    ArticlesClient articles = new ArticlesClient(new Session("", "telifie"));
                    for(Document d : definitions){
                        Article a = new Article(d);
                        Console.message("ID -> " + a.getId());
                        String c = a.getContent();
                        if(!c.contains("_")){
                            String md = c.replace("<em>","_").replace("</em>","_  \n").replace("<p>","").replace("</p>","").replace("</br>","");
                            a.setContent(md);
                            articles.update(a, a);
                        }
                        articles.verify(a.getId());
                    }
                }
            }
        }
    }
}