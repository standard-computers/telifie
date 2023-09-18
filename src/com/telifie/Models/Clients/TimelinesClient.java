package com.telifie.Models.Clients;

import com.mongodb.client.model.UpdateOptions;
import com.telifie.Models.Utilities.Event;
import com.telifie.Models.Actions.Timeline;
import com.telifie.Models.Utilities.Configuration;
import com.telifie.Models.Utilities.Session;
import org.bson.Document;
import java.util.ArrayList;
import java.util.List;

public class TimelinesClient extends Client {

    public TimelinesClient(Configuration config, Session session){
        super(config, session);
        super.collection = "timelines";
    }

    public void addEvents(String object, ArrayList<Event> events){
        this.updateOne(
                new Document("$and",
                        List.of(
                                new Document("object", object)
                        )
                ),
                new Document("$push", new Document("events", Document.parse("{ $each: " + events + "}"))),
                new UpdateOptions().upsert(true)
        );
    }

    public void addEvent(String object, Event event){
        this.updateOne(
                new Document("$and",
                        List.of(
                                new Document("object", object)
                        )
                ),
                new Document("$push", new Document("events", Document.parse(event.toString()))),
                new UpdateOptions().upsert(true)
        );
    }

    public int lastEvent(String object, Event.Type type){
        Document document = this.findOne(new Document("object", object));
        if(document == null){
            return -1;
        }
        Timeline timeline = new Timeline(document);
        int time = (int) (System.currentTimeMillis() / 1000);
        for(int i = timeline.getEvents().size() - 1; i > 0; i--){
            if(timeline.getEvents().get(i).getType() == type){
                return time - timeline.getEvents().get(i).getOrigin();
            }
        }
        return -1;
    }

    public Timeline getTimeline(String objectId) {
        return new Timeline(super.findOne(new Document("object", objectId)));
    }
}