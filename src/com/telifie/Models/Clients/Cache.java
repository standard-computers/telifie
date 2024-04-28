package com.telifie.Models.Clients;

import com.telifie.Models.Utilities.Configuration;
import com.telifie.Models.Utilities.Network.SQL;
import com.telifie.Models.Utilities.Telifie;
import java.sql.*;

public class Cache {

    public void parsed(String user, String url) {
        SQL.update("INSERT INTO parsed (user, uri, origin) VALUES (?, ?, ?)", user, url, String.valueOf(Telifie.epochTime()));
    }

    public boolean isParsed(String url) {
        try {
            PreparedStatement command = Configuration.sqlClient.prepareStatement("SELECT COUNT(*) AS count FROM parsed WHERE uri = ?");
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

    public static String get(String query) {
        try {
            return SQL.get("SELECT response FROM cache WHERE query = ? ORDER BY origin DESC", query).getString("response");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static void invalidate(String query){
        try {
            PreparedStatement ping = Configuration.sqlClient.prepareStatement("DELETE FROM cache WHERE response LIKE '%" + query + "%'");
            ping.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static class history {
        public static boolean isEmpty() {
            boolean empty = true;
            try {
                ResultSet resultSet = Configuration.sqlClient.prepareStatement("SELECT COUNT(*) AS count FROM quickresponse").executeQuery();
                if (resultSet.next()) {
                    int count = resultSet.getInt("count");
                    if (count > 0) {
                        empty = false;
                    }
                }
                resultSet.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return empty;
        }

        public static void log(String user, String obj) {
            try {
                PreparedStatement check = Configuration.sqlClient.prepareStatement("SELECT COUNT(*) FROM pings WHERE user = ? AND object = ?");
                check.setString(1, user);
                check.setString(2, obj);
                ResultSet resultSet = check.executeQuery();
                resultSet.next();
                int count = resultSet.getInt(1);
                if (count > 0) {
                    return;
                }
                SQL.update("INSERT INTO pings (user, object, origin) VALUES (?, ?, ?)", user, obj, String.valueOf(Telifie.epochTime()));
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        public static void commit(String user, String session, String query, String icon, String description){
            SQL.update("INSERT INTO quickresponse (user, session, query, icon, description, origin) VALUES (?, ?, ?, ?, ?, ?)", user, session, query, icon, description, String.valueOf(Telifie.epochTime()));
        }
    }
}