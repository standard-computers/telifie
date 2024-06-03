package com.telifie.Models;

import com.telifie.Models.Utilities.Telifie;
import org.bson.Document;
import java.sql.ResultSet;
import java.sql.SQLException;

public class Shortcut {

    public final String object, name, icon;
    public final int origin;

    public Shortcut(ResultSet rs) throws SQLException {
        object = rs.getString("object");
        name = rs.getString("name");
        icon = rs.getString("icon");
        origin = rs.getInt("origin");
    }

    public Shortcut(Document d){
        this.object = d.getString("object");
        this.name = d.getString("name");
        this.icon = d.getString("icon");
        this.origin = Telifie.epochTime();
    }

    @Override
    public String toString() {
        return "{\"object\" : \"" + object + '\"' +
                ", \"name\" : \"" + name + '\"' +
                ", \"icon\" : \"" + icon + '\"' +
                ", \"origin\" : \"" + origin +"'\"}";
    }
}