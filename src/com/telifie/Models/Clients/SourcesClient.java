package com.telifie.Models.Clients;

import com.telifie.Models.Domain;
import com.telifie.Models.Source;
import org.bson.Document;

import java.util.ArrayList;
import java.util.regex.Pattern;

public class SourcesClient extends Client {

    public SourcesClient(Domain domain) {
        super(domain);
        super.collection = "sources";
    }

    public Source get(String id){

        return new Source(this.findOne(new Document("id", id)));
    }

    public ArrayList<Source> find(String name){

        ArrayList<Document> foundSources = super.find(
                new Document("name", Pattern.compile(Pattern.quote(name), Pattern.CASE_INSENSITIVE))
        );
        ArrayList<Source> sources = new ArrayList<>();
        for(Document doc : foundSources){
            sources.add(new Source(doc));
        }
        return sources;
    }

    public boolean create(Source source){

        return super.insertOne(Document.parse(source.toString()));
    }

}
