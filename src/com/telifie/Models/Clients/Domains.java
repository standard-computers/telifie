package com.telifie.Models.Clients;

import com.telifie.Models.Collection;
import com.telifie.Models.Domain;
import com.telifie.Models.Utilities.Network.SQL;
import com.telifie.Models.Utilities.Session;
import com.telifie.Models.Utilities.Telifie;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;

public class Domains {

    private Session session;

    public Domains(Session session){
        this.session = session;
    }

    public ArrayList<Domain> mine() {
        ArrayList<Domain> domainList = new ArrayList<>();
        HashMap<String, Domain> domainsMap = new HashMap<>();
        try {
            ResultSet domainsResultSet = SQL.get("SELECT * FROM domains WHERE owner = ?", session.user);
            while (domainsResultSet.next()) {
                String domainId = domainsResultSet.getString("id");
                Domain domain = new Domain(domainsResultSet); // Assumes Domain constructor handles domain data
                domainsMap.put(domainId, domain);
                domainList.add(domain);
            }
            ResultSet membershipsResultSet = SQL.get("SELECT m.*, d.id FROM memberships m JOIN domains d ON m.domain = d.id WHERE d.owner = ?", session.user);
            while (membershipsResultSet.next()) {
                String domainId = membershipsResultSet.getString("domain");
                Domain domain = domainsMap.get(domainId);
                if (domain != null) {
                    Domain.Member member = new Domain.Member(membershipsResultSet);
                    domain.getUsers().add(member);
                }
            }
            ResultSet collectionsResultSet = SQL.get("SELECT c.*, d.id FROM collections c JOIN domains d ON c.domain = d.id WHERE d.owner = ?", session.user);
            while (collectionsResultSet.next()) {
                String domainId = collectionsResultSet.getString("domain");
                Domain domain = domainsMap.get(domainId);
                if (domain != null) {
                    Collection collection = new Collection(collectionsResultSet);
                    domain.getCollections().add(collection);
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
        return domainList;
    }


    public boolean delete(Domain d){
        return SQL.delete("DELETE FROM domains WHERE id = ? AND owner = ?", d.id, session.user);
    }

    public boolean create(Domain d){
        return SQL.update("INSERT INTO domains (id, owner, name, alt, permissions, origin) VALUES (?, ?, ?, ?, ?, ?)", d.id, d.owner, d.name, d.alt, d.permissions, Telifie.epochTime());
    }

    public boolean addUser(Domain d, String userId, int permissions){
        return SQL.update("INSERT INTO memberships (user, domain, origin, permissions) VALUES (?, ?, ?, ?)", userId, d.id, Telifie.epochTime(), permissions);
    }

    public boolean removeUser(Domain d, String userId){
        return SQL.delete("DELETE FROM memberships WHERE user = ? AND domain = ?", userId, d.id);
    }

    public boolean updateUser(Domain d, String userId, int permissions){
        return SQL.update("UPDATE memberships SET permissions = ? WHERE domain = ? AND user = ?", permissions, d.id, userId);
    }

    public Domain withId(String id) {
        Domain domain = null;
        try {
            ResultSet domainResultSet = SQL.get("SELECT * FROM domains WHERE id = ? LIMIT 1", id);
            if (domainResultSet.next()) {
                domain = new Domain(domainResultSet);
            }
            if (domain != null) {
                ResultSet membershipsResultSet = SQL.get("SELECT * FROM memberships WHERE domain = ?", id);
                while (membershipsResultSet.next()) {
                    Domain.Member member = new Domain.Member(membershipsResultSet);
                    domain.getUsers().add(member);
                }
                ResultSet collectionsResultSet = SQL.get("SELECT * FROM collections WHERE domain = ?", id);
                while (collectionsResultSet.next()) {
                    Collection collection = new Collection(collectionsResultSet);
                    domain.getCollections().add(collection);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
        return domain;
    }
}