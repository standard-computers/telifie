package com.telifie.Models.Utilities.Network;

import com.telifie.Models.Utilities.Configuration;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class SQL {
    public static boolean update(String query, Object... params) {
        try {
            PreparedStatement statement = Configuration.mysqlClient.prepareStatement(query);
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
        PreparedStatement statement = Configuration.mysqlClient.prepareStatement(query);
        for (int i = 0; i < params.length; i++) {
            statement.setObject(i + 1, params[i]);
        }
        return statement.executeQuery();
    }
}
