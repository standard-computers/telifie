package com.telifie.Models.Clients;

import com.mongodb.client.model.UpdateOptions;
import com.telifie.Models.Utilities.Event;
import com.telifie.Models.Actions.Timeline;
import com.telifie.Models.Utilities.Session;
import org.bson.Document;
import java.util.List;

public class TimelinesClient extends Client {

    public TimelinesClient(Session session){
        super(session);
        super.collection = "timelines";
    }

    public void addEvent(String object, Event.Type event){
        this.updateOne(new Document("$and", List.of(
                new Document("object", object))),
                new Document("$push",
                        new Document("events", event.toString())
                ), new UpdateOptions().upsert(true)
        );
    }

    public Timeline getTimeline(String objectId) {
        return new Timeline(super.findOne(new Document("object", objectId)));
    }
}