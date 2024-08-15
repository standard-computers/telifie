package com.telifie.Models;

import com.telifie.Models.Utilities.Telifie;
import org.bson.Document;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

public class Index {

    public final String id, domain, name, alias;
    public final int origin;

    public Index(ResultSet set) throws SQLException {
        this.id = set.getString("id");
        this.domain = set.getString("domain");
        this.name = set.getString("name");
        this.alias = set.getString("alias");
        this.origin = set.getInt("origin");
    }

    public Index(Document document) {
        this.id = UUID.randomUUID().toString();
        this.domain = document.getString("domain");
        this.name = document.getString("name");
        this.alias = this.name.toLowerCase().trim().replaceAll(" ", "_");
        this.origin = Telifie.time();
    }

    @Override
    public String toString() {
        return "{\"id\" : \"" + id + '\"' +
                ", \"name\" : \"" + name + '\"' +
                ", \"alias\" : \"" + alias + '\"' +
                ", \"origin\" :" + origin +
                '}';
    }
}