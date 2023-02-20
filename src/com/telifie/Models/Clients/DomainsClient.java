package com.telifie.Models.Clients;

import com.telifie.Models.Domain;
import org.bson.Document;
import java.util.ArrayList;
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

        return this.insertOne(Document.parse(domain.toString()));

    }

}
