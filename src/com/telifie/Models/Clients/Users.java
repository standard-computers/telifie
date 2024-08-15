package com.telifie.Models.Clients;

import com.telifie.Models.User;
import com.telifie.Models.Utilities.Network.SQL;
import java.sql.ResultSet;
import java.sql.SQLException;

public class Users {

    public User getUserWithEmail(String email){
        try {
            ResultSet resultSet = SQL.get("SELECT * FROM users WHERE email = ?", email);
            if (resultSet.next()) {
                return new User(resultSet);
            } else return null;
        } catch (SQLException e) {
            return null;
        }
    }

    public User getUserWithId(String id){
        try {
            ResultSet resultSet = SQL.get("SELECT * FROM users WHERE id = ?", id);
            if (resultSet.next()) {
                return new User(resultSet);
            } else return null;
        } catch (SQLException e) {
            return null;
        }
    }

    public User getUserWithPhone(String phone){
        try {
            ResultSet resultSet = SQL.get("SELECT * FROM users WHERE phone = ?", phone);
            if (resultSet.next()) {
                return new User(resultSet);
            } else return null;
        } catch (SQLException e) {
            return null;
        }
    }

    public boolean existsWithEmail(String email){
        return (this.getUserWithEmail(email) != null);
    }
}