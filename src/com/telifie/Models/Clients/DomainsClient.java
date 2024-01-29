package com.telifie.Models.Clients;

import com.telifie.Models.Domain;
import com.telifie.Models.Member;
import com.telifie.Models.Utilities.Session;
import org.bson.Document;
import java.util.ArrayList;
import java.util.Arrays;

public class DomainsClient extends Client {

    public DomainsClient(Session session){
        super(session);
        this.collection = "domains";
    }

    public ArrayList<Domain> mine(){
        return super.find(new Document("owner", session.user)).map(Domain::new).into(new ArrayList<>());
    }

    public boolean delete(Domain domain){
        return super.deleteOne(new Document("$and", Arrays.asList(new Document("owner", session.user), new Document("id", domain.getId()))));
    }

    public ArrayList<Domain> forMember(String userId){
        return super.find(new Document("$or", Arrays.asList(new Document("owner", userId), new Document("users.email", userId)))).map(Domain::new).into(new ArrayList<>());
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