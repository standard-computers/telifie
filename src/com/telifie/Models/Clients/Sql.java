package com.telifie.Models.Clients;

import com.telifie.Models.Utilities.Configuration;
import com.telifie.Models.Utilities.Telifie;
import java.sql.*;

public class Sql {

    private static Connection sql;

    public Sql(){
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            this.sql = DriverManager.getConnection(Configuration.mysql.getUri(), Configuration.mysql.getUser(), Configuration.mysql.getPsswd());
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void parsed(String user, String url) {
        String insert = "INSERT INTO parsed (user, uri, origin) VALUES (?, ?, ?)";
        try {
            PreparedStatement command = this.sql.prepareStatement(insert);
            command.setString(1, user);
            command.setString(2, url);
            command.setString(3, String.valueOf(Telifie.epochTime()));
            command.execute();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean isParsed(String url) {
        String query = "SELECT COUNT(*) AS count FROM parsed WHERE uri = ?";
        try {
            PreparedStatement command = this.sql.prepareStatement(query);
            command.setString(1, url);
            ResultSet resultSet = command.executeQuery();
            if (resultSet.next()) {
                int count = resultSet.getInt("count");
                return count > 0;
            }
            return false;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
