package com.telifie.Models.Clients;

import com.telifie.Models.Domain;
import com.telifie.Models.Index;
import com.telifie.Models.Utilities.Network.SQL;
import java.sql.ResultSet;
import java.sql.SQLException;

public class Indexes {

    public static boolean create(Domain d, Index i){
        return SQL.update("INSERT INTO indexes (id, domain, name, alias, origin) VALUES (?, ?, ?, ?, ?)", i.id, i.domain, i.name, i.alias, i.origin);
    }

    public static Index withAlias(String domainId, String alias){
        try {
            ResultSet resultSet = SQL.get("SELECT * FROM indexes WHERE domain = ? and alias = ? LIMIT 1", domainId, alias);
            if (resultSet.next()) {
                return new Index(resultSet);
            } else return null;
        } catch (SQLException e) {
            return null;
        }
    }

    public static Index get(String indexId){
        try {
            ResultSet resultSet = SQL.get("SELECT * FROM indexes WHERE id = ? LIMIT 1", indexId);
            if (resultSet.next()) {
                return new Index(resultSet);
            } else return null;
        } catch (SQLException e) {
            return null;
        }
    }

    public static boolean delete(Domain d, Index i){
        return SQL.delete("DELETE FROM indexes WHERE id = ? AND domain = ?", i.id, d.id);
    }
}