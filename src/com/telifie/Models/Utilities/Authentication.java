package com.telifie.Models.Utilities;

import com.telifie.Models.User;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class Authentication {

    private final String user, token, refresh;
    private int origin, expiration;

    public Authentication(String bearer){
        String[] b = bearer.split(" ")[1].split("\\.");
        this.user = b[0];
        this.token = b[1];
        this.refresh = b[2];
    }

    public Authentication(User user){
        this.user = user.getId();
        this.token = Telifie.md5(Telifie.randomReferenceCode());
        this.refresh = Telifie.md5(Telifie.randomReferenceCode());
        this.origin = Telifie.epochTime();
        this.expiration = this.origin + 604800;
    }

    public String getUser() {
        return user;
    }

    public boolean authenticate(){
        try {
            PreparedStatement command = Configuration.sqlClient.prepareStatement("INSERT INTO authentications (user, token, refresh, origin, expiration) VALUES (?, ?, ?, ?, ?)");
            command.setString(1, user);
            command.setString(2, token);
            command.setString(3, refresh);
            command.setString(4, String.valueOf(origin));
            command.setString(5, String.valueOf(expiration));
            command.execute();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean isAuthenticated() {
        try {
            PreparedStatement command = Configuration.sqlClient.prepareStatement("SELECT * FROM authentications WHERE user = ? AND token = ? AND refresh = ? AND expiration > ? LIMIT 1");
            command.setString(1, user);
            command.setString(2, token);
            command.setString(3, refresh);
            command.setInt(4, Telifie.epochTime());
            ResultSet resultSet = command.executeQuery();
            return resultSet.next();
        } catch (SQLException e) {
            Log.error("Failed authentication", "AUTHx102");
            return false;
        }
    }

    @Override
    public String toString() {
        return new StringBuilder().append("{").append("\"user\" : \"").append(user).append('\"').append(", \"token\" : \"").append(token).append('\"').append(", \"refresh\" : \"").append(refresh).append('\"').append(", \"origin\" : ").append(origin).append(", \"expiration\" : ").append(expiration).append("}").toString();
    }
}