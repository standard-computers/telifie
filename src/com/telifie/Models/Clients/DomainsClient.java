package com.telifie.Models.Clients;

import com.telifie.Models.Domain;
import com.telifie.Models.Member;
import com.telifie.Models.Utilities.Configuration;
import com.telifie.Models.Utilities.Session;
import org.bson.Document;
import java.util.ArrayList;
import java.util.Arrays;

public class DomainsClient extends Client {

    public DomainsClient(Configuration config, Session session){
        super(config, session);
        this.collection = "domains";
    }

    /**
     * Returns only the domains the user id owns provided by Configuration
     * @return ArrayList<Domain>
     */
    public ArrayList<Domain> mine(){
        ArrayList<Document> found = super.find(new Document("owner", session.getUser()));
        ArrayList<Domain> domains = new ArrayList<>();
        for(Document doc : found){
            domains.add(new Domain(doc));
        }
        return domains;
    }

    public boolean delete(Domain domain){
        return super.deleteOne(new Document("$and", Arrays.asList(new Document("owner", session.getUser()), new Document("id", domain.getId()))));
    }

    /**
     * Gets domains for user id, either the ones they own,
     * or the one's they're a part of.
     * @param userId ID of user
     * @return ArrayList<Domain>
     */
    public ArrayList<Domain> forMember(String userId){
        ArrayList<Domain> domains = new ArrayList<>();
        ArrayList<Document> fnd = super.find(new Document("$or", Arrays.asList(
                    new Document("owner", userId),
                    new Document("users.email", userId)
                )
            )
        );
        fnd.forEach(doc -> domains.add(new Domain(doc)));
        return domains;
    }

    public boolean create(Domain domain){
        return this.insertOne(Document.parse(domain.toString()));
    }

    public boolean addUsers(Domain domain, ArrayList<Member> members){
        members.forEach(member -> super.updateOne(new Document("id", domain.getId()), new Document("$push", new Document("users", Document.parse(member.toString())))));
        return true;
    }

    public boolean removeUsers(Domain domain, ArrayList<Member> members){
        members.forEach(member -> super.updateOne(new Document("id", domain.getId()), new Document("$pull", new Document("users", Document.parse(member.toString())))));
        return true;
    }

    public boolean updateUsers(Domain domain, ArrayList<Member> members){
        members.forEach(member -> super.updateOne(new Document("id", domain.getId()), new Document("$set", new Document("users", Document.parse(member.toString())))));
        return true;
    }
    
    public Domain withId(String id){
        return new Domain(super.findOne(new Document("id", id)));
    }

    public boolean update(Domain domain, Document updates){
        return super.updateOne(new Document("id", domain.getId()), new Document("$set", updates));
    }
}