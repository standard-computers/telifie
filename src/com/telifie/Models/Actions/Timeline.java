package com.telifie.Models.Actions;

import org.bson.Document;
import java.util.ArrayList;

public class Timeline {

    private final String object;
    private final ArrayList<Event> events = new ArrayList<>();

    public Timeline(Document document){
        this.object = document.getString("object");
        ArrayList<Document> eventsData = (ArrayList<Document>) document.getList("events", Document.class);
        if(eventsData != null){
            eventsData.forEach(d -> events.add(new Event(d)));
        }
    }

    @Override
    public String toString() {
        return "{\"object\" : \"" + object + '\"' +
                ", \"events\" :" + events + '}';
    }
}
