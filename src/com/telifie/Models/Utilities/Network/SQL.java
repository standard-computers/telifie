package com.telifie.Models.Utilities.Network;

import com.telifie.Models.Utilities.Configuration;
import com.telifie.Models.Utilities.Telifie;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class SQL {
    public static boolean update(String query, Object... params) {
        try {
            PreparedStatement statement = Configuration.sqlClient.prepareStatement(query);
            for (int i = 0; i < params.length; i++) {
                statement.setObject(i + 1, params[i]);
            }
            int rowsAffected = statement.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static ResultSet get(String query, Object... params) throws SQLException {
        PreparedStatement statement = Configuration.sqlClient.prepareStatement(query);
        for (int i = 0; i < params.length; i++) {
            statement.setObject(i + 1, params[i]);
        }
        return statement.executeQuery();
    }

    public static boolean delete(String query, Object... params) {
        try {
            PreparedStatement statement = Configuration.sqlClient.prepareStatement(query);
            for (int i = 0; i < params.length; i++) {
                statement.setObject(i + 1, params[i]);
            }
            int rowsAffected = statement.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public void parsed(String user, String url) {
        SQL.update("INSERT INTO parsed (user, uri, origin) VALUES (?, ?, ?)", user, url, String.valueOf(Telifie.time()));
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
            SQL.update("INSERT INTO pings (user, object, origin) VALUES (?, ?, ?)", user, obj, String.valueOf(Telifie.time()));
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}