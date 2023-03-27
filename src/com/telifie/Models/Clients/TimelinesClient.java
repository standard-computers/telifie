package com.telifie.Models.Clients;

import com.mongodb.client.model.UpdateOptions;
import com.telifie.Models.Actions.Event;
import com.telifie.Models.Actions.Timeline;
import com.telifie.Models.Domain;
import com.telifie.Models.Utilities.Configuration;
import org.bson.Document;
import java.util.ArrayList;
import java.util.Arrays;

public class TimelinesClient extends Client {

    public TimelinesClient(Configuration config){

        super(config);
        super.collection = "timelines";
    }

    public boolean addEvents(String object, ArrayList<Event> events){
        return this.updateOne(
                new Document("$and",
                        Arrays.asList(
                                new Document("object", object)
                        )
                ),
                new Document("$push", new Document("events", Document.parse("{ $each: " + events + "}"))),
                new UpdateOptions().upsert(true)
        );
    }

    public Timeline getTimeline(String objectId) {
        return new Timeline(super.findOne(new Document("object", objectId)));
    }

}
