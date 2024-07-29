package com.telifie.Models.Clients;

import com.telifie.Models.Article;
import com.telifie.Models.Domain;
import com.telifie.Models.Index;
import com.telifie.Models.Utilities.Configuration;
import com.telifie.Models.Utilities.Network.SQL;
import com.telifie.Models.Utilities.Session;
import org.bson.Document;
import java.sql.ResultSet;
import java.sql.SQLException;

public class Indexes {

    private Session session;

    public Indexes(Session session) {
        this.session = session;
    }

    public boolean create(Domain d, Index i){
        Configuration.mongoClient.getDatabase(d.alias).getCollection(i.alias);
        session.setDomain(d.alias);
        Articles articles = new Articles(session, i.alias);
        articles.create(new Article(new Document("title", "Welcome to your new Index!").append("link", "https://telifie.com/manual")));
        return SQL.update("INSERT INTO indexes (id, domain, name, alias, origin) VALUES (?, ?, ?, ?, ?)", i.id, i.domain, i.name, i.alias, i.origin);
    }

    public Index withAlias(String domainId, String alias){
        try {
            ResultSet resultSet = SQL.get("SELECT * FROM indexes WHERE domain = ? and alias = ? LIMIT 1", domainId, alias);
            if (resultSet.next()) {
                return new Index(resultSet);
            } else return null;
        } catch (SQLException e) {
            return null;
        }
    }

    public Index get(String indexId){
        try {
            ResultSet resultSet = SQL.get("SELECT * FROM indexes WHERE id = ? LIMIT 1", indexId);
            if (resultSet.next()) {
                return new Index(resultSet);
            } else return null;
        } catch (SQLException e) {
            return null;
        }
    }

    public boolean delete(Domain d, Index i){
        Configuration.mongoClient.getDatabase(d.alias).getCollection(i.alias).drop();
        return SQL.delete("DELETE FROM indexes WHERE id = ? AND domain = ?", i.id, d.id);
    }
}