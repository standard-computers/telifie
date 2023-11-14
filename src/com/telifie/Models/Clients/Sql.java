package com.telifie.Models.Clients;

import com.telifie.Models.Utilities.Configuration;
import com.telifie.Models.Utilities.Telifie;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class Sql {

    private Connection sql;

    public Sql() throws SQLException {
        this.sql = DriverManager.getConnection(Configuration.mysql.getUri(), Configuration.mysql.getUser(), Configuration.mysql.getPsswd());
    }

    public void queue(String user, String url) throws SQLException {
        String insert = "INSERT INTO queue (user, uri, origin) VALUES (?, ?, ?)";
        PreparedStatement command = this.sql.prepareStatement(insert);
        command.setString(1, user);
        command.setString(2, url);
        command.setString(2, String.valueOf(Telifie.epochTime()));
        command.execute();
    }

}
