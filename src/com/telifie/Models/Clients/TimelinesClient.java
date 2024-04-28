package com.telifie.Models.Clients;

import com.telifie.Models.Utilities.Event;
import com.telifie.Models.Utilities.Session;
import org.bson.Document;
import java.util.ArrayList;

public class TimelinesClient extends Client {

    public TimelinesClient(Session session){
        super(session);
        super.collection = "timelines";
    }

    public Timeline getTimeline(String objectId) {
        return new Timeline(super.findOne(new Document("object", objectId)));
    }

    public class Timeline {
        public Timeline(Document document){
            ArrayList<Event> events = new ArrayList<>();
            ArrayList<Document> eventsData = (ArrayList<Document>) document.getList("events", Document.class);
            if(eventsData != null){
                eventsData.forEach(d -> events.add(new Event(d)));
            }
        }
    }
}