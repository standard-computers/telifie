package com.telifie.Models.Clients;

import com.telifie.Models.Connectors.SendGrid;
import com.telifie.Models.Connectors.Twilio;
import com.telifie.Models.User;
import com.telifie.Models.Utilities.Network.SQL;
import com.telifie.Models.Utilities.Telifie;
import java.sql.ResultSet;
import java.sql.SQLException;

public class UsersClient {

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

    public boolean lock(User user, String code) {
        return SQL.update("UPDATE users SET token = ? WHERE email = ?", Telifie.md5(code), user.getEmail());
    }

    public boolean emailCode(User user){
        String code = Telifie.digitCode();
        SendGrid.sendCode(user.getEmail(), code);
        return this.lock(user, code);
    }

    public boolean textCode(User user){
        String code = Telifie.digitCode();
        Twilio.send(user.getPhone(), "+15138029566", "Hello \uD83D\uDC4B It's Telifie! Your login code is " + code);
        return this.lock(user, code);
    }


    public boolean updateSettings(User user, String settings) {
        return SQL.update("UPDATE users SET settings = ? WHERE id = ?", settings, user.getId());
    }

    public boolean upgradePermissions(User user, int permissions) {
        return SQL.update("UPDATE users SET permissions = ? WHERE id = ?", permissions, user.getId());
    }
}