package com.telifie.Models.Clients;

import com.telifie.Models.Index;
import com.telifie.Models.Domain;
import com.telifie.Models.Utilities.Network.SQL;
import com.telifie.Models.Utilities.Session;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;

public class Domains {

    private final Session session;

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
                Domain domain = new Domain(drs);
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
            return null;
        }
        return domainList;
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
            return null;
        }
        return domain;
    }
}