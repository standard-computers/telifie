package com.telifie.Models;

import com.telifie.Models.Utilities.Telifie;
import org.bson.Document;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

public class Collection {

    public final String id, domain, name, alt;
    public final int origin;

    public Collection(ResultSet set) throws SQLException {
        this.id = set.getString("id");
        this.domain = set.getString("domain");
        this.name = set.getString("name");
        this.alt = set.getString("alt");
        this.origin = set.getInt("origin");
    }

    public Collection(Document document) {
        this.id = UUID.randomUUID().toString();
        this.domain = document.getString("domain");
        this.name = document.getString("name");
        this.alt = this.name.toLowerCase().trim().replaceAll(" ", "_");
        this.origin = Telifie.epochTime();
    }

    @Override
    public String toString() {
        return "{" +
                "\"id\" : \"" + id + '\"' +
                ", \"name\" : \"" + name + '\"' +
                ", \"alt\" : \"" + alt + '\"' +
                ", \"origin\" :" + origin +
                '}';
    }
}