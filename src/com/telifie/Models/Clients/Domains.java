package com.telifie.Models.Clients;

import com.telifie.Models.Index;
import com.telifie.Models.Domain;
import com.telifie.Models.Utilities.Network.SQL;
import java.sql.ResultSet;
import java.sql.SQLException;

public class Domains {

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

//        return SQL.update("INSERT INTO indexes (id, domain, name, alias, origin) VALUES (?, ?, ?, ?, ?)", i.id, i.domain, i.name, i.alias, i.origin);
//        return SQL.delete("DELETE FROM indexes WHERE id = ? AND domain = ?", i.id, d.id);
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