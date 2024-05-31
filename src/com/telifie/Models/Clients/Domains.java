package com.telifie.Models.Clients;

import com.telifie.Models.Index;
import com.telifie.Models.Domain;
import com.telifie.Models.Utilities.Network.SQL;
import com.telifie.Models.Utilities.Session;
import com.telifie.Models.Utilities.Telifie;
import org.bson.Document;
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
            ResultSet drs = SQL.get("SELECT * FROM domains WHERE owner = ?", session.user);
            while (drs.next()) {
                String domainId = drs.getString("id");
                Domain domain = new Domain(drs); // Assumes Domain constructor handles domain data
                domainsMap.put(domainId, domain);
                domainList.add(domain);
            }
            ResultSet mrs = SQL.get("SELECT m.*, d.id FROM memberships m JOIN domains d ON m.domain = d.id WHERE d.owner = ?", session.user);
            while (mrs.next()) {
                String domainId = mrs.getString("domain");
                Domain domain = domainsMap.get(domainId);
                if (domain != null) {
                    Domain.Member member = new Domain.Member(mrs);
                    domain.getUsers().add(member);
                }
            }
            ResultSet irs = SQL.get("SELECT c.*, d.id FROM indexes c JOIN domains d ON c.domain = d.id WHERE d.owner = ?", session.user);
            while (irs.next()) {
                String domainId = irs.getString("domain");
                Domain domain = domainsMap.get(domainId);
                if (domain != null) {
                    Index index = new Index(irs);
                    domain.getIndexes().add(index);
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
        return domainList;
    }

    public ArrayList<Domain> viewable() {
        ArrayList<Domain> domainList = new ArrayList<>();
        HashMap<String, Domain> domainsMap = new HashMap<>();
        try {
            ResultSet drs = SQL.get("SELECT * FROM domains WHERE permissions = 1");
            while (drs.next()) {
                String domainId = drs.getString("id");
                Domain domain = new Domain(drs); // Assumes Domain constructor handles domain data
                domainsMap.put(domainId, domain);
                domainList.add(domain);
            }
            ResultSet mrs = SQL.get("SELECT m.*, d.id FROM memberships m JOIN domains d ON m.domain = d.id WHERE d.owner = ?", session.user);
            while (mrs.next()) {
                String domainId = mrs.getString("domain");
                Domain domain = domainsMap.get(domainId);
                if (domain != null) {
                    Domain.Member member = new Domain.Member(mrs);
                    domain.getUsers().add(member);
                }
            }
            ResultSet irs = SQL.get("SELECT c.*, d.id FROM indexes c JOIN domains d ON c.domain = d.id WHERE d.owner = ?", session.user);
            while (irs.next()) {
                String domainId = irs.getString("domain");
                Domain domain = domainsMap.get(domainId);
                if (domain != null) {
                    Index index = new Index(irs);
                    domain.getIndexes().add(index);
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
        if(SQL.update("INSERT INTO domains (id, owner, name, alias, permissions, origin) VALUES (?, ?, ?, ?, ?, ?)", d.id, d.owner, d.name, d.alias, d.permissions, Telifie.epochTime())){
            Indexes indexes = new Indexes(session);
            indexes.create(d, new Index(new Document("domain", d.id).append("name", "Articles")));
            session.setDomain(d.alias);
            return true;
        }
        return false;
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
            ResultSet drs = SQL.get("SELECT * FROM domains WHERE id = ? LIMIT 1", id);
            if (drs.next()) {
                domain = new Domain(drs);
            }
            if (domain != null) {
                ResultSet mrs = SQL.get("SELECT * FROM memberships WHERE domain = ?", id);
                while (mrs.next()) {
                    Domain.Member member = new Domain.Member(mrs);
                    domain.getUsers().add(member);
                }
                ResultSet irs = SQL.get("SELECT * FROM indexes WHERE domain = ?", id);
                while (irs.next()) {
                    Index index = new Index(irs);
                    domain.getIndexes().add(index);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
        return domain;
    }

    public Domain withAlias(String alias) {
        Domain domain = null;
        try {
            ResultSet drs = SQL.get("SELECT * FROM domains WHERE alias = ? LIMIT 1", alias);
            if (drs.next()) {
                domain = new Domain(drs);
            }
            if (domain != null) {
                ResultSet mrs = SQL.get("SELECT * FROM memberships WHERE domain = ?", domain.id);
                while (mrs.next()) {
                    Domain.Member member = new Domain.Member(mrs);
                    domain.getUsers().add(member);
                }
                ResultSet crs = SQL.get("SELECT * FROM indexes WHERE domain = ?", domain.id);
                while (crs.next()) {
                    Index index = new Index(crs);
                    domain.getIndexes().add(index);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
        return domain;
    }
}