package com.telifie.Models.Clients;

import com.mongodb.MongoException;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import com.telifie.Models.Domain;
import com.telifie.Models.Member;
import com.telifie.Models.Utilities.Configuration;
import org.bson.Document;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DomainsClient extends Client {

    public DomainsClient(Configuration config){
        super(config);
        this.collection = "domains";
    }

    /**
     * Returns only the domains the user id owns
     * @param userId ID of user
     * @return ArrayList<Domain>
     */
    public ArrayList<Domain> ownedDomains(String userId){
        ArrayList<Document> found = super.find(new Document("owner", userId));
        ArrayList<Domain> domains = new ArrayList<>();
        for(Document doc : found){
            domains.add(new Domain(doc));
        }
        return domains;
    }

    public Domain withId(String id){
        return new Domain(super.findOne(new Document("id", id)));
    }

    public boolean delete(String owner, String id){
        return super.deleteOne(new Document("$and", Arrays.asList(new Document("owner", owner), new Document("id", id))));
    }

    /**
     * Gets domains for user id, either the ones they own,
     * or the one's they're a part of.
     * @param userId ID of user
     * @return ArrayList<Domain>
     */
    public ArrayList<Domain> forUser(String userId){
        ArrayList<Domain> domains = new ArrayList<>();
        super.find(new Document("$and", List.of(new Document("owner", userId))));
        return domains;
    }

    public boolean create(Domain domain){
        if(this.insertOne(Document.parse(domain.toString()))){
            try(MongoClient mongoClient = MongoClients.create(super.config.getDomain().getUri())){
                MongoDatabase database = mongoClient.getDatabase("domains-articles");
                database.createCollection(domain.getAlt());
            }catch(MongoException e){
                return true;
            }
            return true;
        }
        return false;
    }

    public boolean addUsers(String domain, ArrayList<Member> members){
        members.forEach(member -> super.updateOne(new Document("id", domain), new Document("$push", new Document("users", Document.parse(member.toString())))));
        return true;
    }

    public boolean removeUsers(String domain, ArrayList<Member> members){
        members.forEach(member -> super.updateOne(new Document("id", domain), new Document("$pull", new Document("users", Document.parse(member.toString())))));
        return true;
    }
}