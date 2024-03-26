package com.telifie.Models.Clients;

import com.telifie.Models.Utilities.Configuration;
import com.telifie.Models.Utilities.Telifie;
import java.sql.*;

public class Sql {

    public void parsed(String user, String url) {
        try {
            PreparedStatement command = Configuration.mysqlClient.prepareStatement("INSERT INTO parsed (user, uri, origin) VALUES (?, ?, ?)");
            command.setString(1, user);
            command.setString(2, url);
            command.setString(3, String.valueOf(Telifie.epochTime()));
            command.execute();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public boolean isParsed(String url) {
        try {
            PreparedStatement command = Configuration.mysqlClient.prepareStatement("SELECT COUNT(*) AS count FROM parsed WHERE uri = ?");
            command.setString(1, url);
            ResultSet resultSet = command.executeQuery();
            if (resultSet.next()) {
                int count = resultSet.getInt("count");
                return count > 0;
            }
            return false;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static void ping(String user, String articleId) {
        try {
            PreparedStatement checkStatement = Configuration.mysqlClient.prepareStatement("SELECT COUNT(*) FROM pings WHERE user = ? AND object = ?");
            checkStatement.setString(1, user);
            checkStatement.setString(2, articleId);
            ResultSet resultSet = checkStatement.executeQuery();
            resultSet.next();
            int count = resultSet.getInt(1);
            if (count > 0) {
                return;
            }
            PreparedStatement insertStatement = Configuration.mysqlClient.prepareStatement("INSERT INTO pings (user, object, origin) VALUES (?, ?, ?)");
            insertStatement.setString(1, user);
            insertStatement.setString(2, articleId);
            insertStatement.setString(3, String.valueOf(Telifie.epochTime()));
            insertStatement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}