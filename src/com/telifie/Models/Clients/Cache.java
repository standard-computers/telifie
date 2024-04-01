package com.telifie.Models.Clients;

import com.telifie.Models.Utilities.Configuration;
import com.telifie.Models.Utilities.Parameters;
import com.telifie.Models.Utilities.Telifie;
import java.sql.*;
import java.util.UUID;

public class Cache {

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
            PreparedStatement check = Configuration.mysqlClient.prepareStatement("SELECT COUNT(*) FROM pings WHERE user = ? AND object = ?");
            check.setString(1, user);
            check.setString(2, articleId);
            ResultSet resultSet = check.executeQuery();
            resultSet.next();
            int count = resultSet.getInt(1);
            if (count > 0) {
                return;
            }
            PreparedStatement ping = Configuration.mysqlClient.prepareStatement("INSERT INTO pings (user, object, origin) VALUES (?, ?, ?)");
            ping.setString(1, user);
            ping.setString(2, articleId);
            ping.setString(3, String.valueOf(Telifie.epochTime()));
            ping.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static String fromCache(String query) {
        String cachedResponse = null;
        try {
            PreparedStatement command = Configuration.mysqlClient.prepareStatement("SELECT response FROM cache WHERE query = ? ORDER BY origin DESC");
            command.setString(1, query);
            ResultSet resultSet = command.executeQuery();
            if (resultSet.next()) {
                cachedResponse = resultSet.getString("response");
            }
            resultSet.close();
            command.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return cachedResponse;
    }

    public static void cache(String user, String query, String result, Parameters params){
        try {
            PreparedStatement ping = Configuration.mysqlClient.prepareStatement("INSERT INTO cache (user, session, query, page, rpp, pages, response, origin) VALUES (?, ?, ?, ?, ?, ?, ?, ?)");
            ping.setString(1, user);
            ping.setString(2, UUID.randomUUID().toString());
            ping.setString(3, query);
            ping.setInt(4, params.page);
            ping.setInt(5, params.rpp);
            ping.setInt(6, params.pages);
            ping.setString(7, result);
            ping.setString(8, String.valueOf(Telifie.epochTime()));
            ping.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}