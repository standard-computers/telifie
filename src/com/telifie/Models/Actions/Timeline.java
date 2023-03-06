package com.telifie.Models.Actions;

import org.bson.Document;

import java.util.ArrayList;

public class Timeline {

    private String object;
    private ArrayList<Event> events = new ArrayList<>();

    public Timeline(String objectId) {
        this.object = objectId;
    }

    public Timeline(Document document){

        this.object = document.getString("object");
        ArrayList<Document> eventsData = (ArrayList<Document>) document.getList("events", Document.class);
        if(eventsData != null){

            for (Document doc: eventsData) {

                events.add(new Event(doc));
            }
        }
    }

    public String getObject() {
        return object;
    }

    public ArrayList<Event> getEvents() {

        return events;
    }

    @Override
    public String toString() {

        return "{\"object\" : \"" + object + '\"' +
                ", \"events\" :" + events + '}';
    }
}
