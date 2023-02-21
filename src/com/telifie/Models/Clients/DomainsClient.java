package com.telifie.Models.Clients;

import com.mongodb.MongoException;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.telifie.Models.Domain;
import org.bson.Document;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DomainsClient extends Client {

    /**
     * Creates a DomainsClient for interfacing with domains in database.
     * @param domain Working domain that contains to domains to interface with
     */
    public DomainsClient(Domain domain){
        super(domain);
        this.collection = "domains";
    }

    /**
     * Returns only the domains the user id owns
     * @param userId ID of user
     * @return ArrayList<Domain>
     */
    public ArrayList<Domain> getOwnedDomains(String userId){

        ArrayList<Document> found = super.find(new Document("owner", userId));
        ArrayList<Domain> domains = new ArrayList<>();
        for(Document doc : found){
            domains.add(new Domain(doc));
        }

        return domains;
    }

    public Domain getWithId(String id){
        return new Domain(super.findOne(new Document("id", id)));
    }

    public boolean delete(String owner, String id){
        return super.deleteOne(
            new Document("$and",
                Arrays.asList(
                    new Document("owner", owner),
                    new Document("id", id)
                )
            )
        );
    }

    /**
     * Gets domains for user id, either the ones they own,
     * or the one's they're a part of.
     * @param userId ID of user
     * @return ArrayList<Domain>
     */
    public ArrayList<Domain> getDomainsForUser(String userId){

        ArrayList<Domain> domains = new ArrayList<>();
        super.find(
                new Document("$and", List.of(
                        new Document("owner", userId)
                )
            )
        );

        return domains;
    }

    public boolean create(Domain domain){

        if(this.insertOne(Document.parse(domain.toString()))){

            //Now setup collections in the databases to be prepared for use.
            try(MongoClient mongoClient = MongoClients.create(super.domain.getUri())){

                MongoDatabase database = mongoClient.getDatabase("domains-articles");
                database.createCollection(domain.getAlt());

            }catch(MongoException e){

                return true;
            }

            return true;

        }else{

            return false;
        }

    }
}