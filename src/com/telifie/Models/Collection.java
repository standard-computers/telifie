package com.telifie.Models;

import java.sql.ResultSet;
import java.sql.SQLException;

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
}