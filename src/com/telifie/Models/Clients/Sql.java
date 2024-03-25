package com.telifie.Models.Clients;

import com.telifie.Models.Utilities.Configuration;
import com.telifie.Models.Utilities.Console;
import com.telifie.Models.Utilities.Telifie;
import java.sql.*;

public class Sql {

    private static Connection sql;

    public Sql(){
        String url = "jdbc:mysql://" + Configuration.mysql.getUri() + ":3306/telifie";
        try (Connection connection = DriverManager.getConnection(url, Configuration.mysql.getUser(), Configuration.mysql.getPsswd())) {
            System.out.println("Connected to the MySQL database!");
            sql = connection;
        } catch (SQLException e) {
            System.out.println("Connection failed!");
            e.printStackTrace();
        }
    }

    public void parsed(String user, String url) {
        try {
            PreparedStatement command = this.sql.prepareStatement("INSERT INTO parsed (user, uri, origin) VALUES (?, ?, ?)");
            command.setString(1, user);
            command.setString(2, url);
            command.setString(3, String.valueOf(Telifie.epochTime()));
            command.execute();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean isParsed(String url) {
        try {
            PreparedStatement command = this.sql.prepareStatement("SELECT COUNT(*) AS count FROM parsed WHERE uri = ?");
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

    public void queue(String user, String url) {
        Console.log("QUEUEING -> " + url);
        try {
            PreparedStatement command = this.sql.prepareStatement("INSERT INTO queue (user, uri, origin) VALUES (?, ?, ?)");
            command.setString(1, user);
            command.setString(2, url);
            command.setString(3, String.valueOf(Telifie.epochTime()));
            command.execute();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static void ping(String user, String articleId) {
        String url = "jdbc:mysql://" + Configuration.mysql.getUri() + ":3306/telifie";
        try (Connection connection = DriverManager.getConnection(url, Configuration.mysql.getUser(), Configuration.mysql.getPsswd())) {
            PreparedStatement checkStatement = connection.prepareStatement("SELECT COUNT(*) FROM pings WHERE user = ? AND object = ?");
            checkStatement.setString(1, user);
            checkStatement.setString(2, articleId);
            ResultSet resultSet = checkStatement.executeQuery();
            resultSet.next();
            int count = resultSet.getInt(1);
            if (count > 0) {
                return;
            }
            PreparedStatement insertStatement = connection.prepareStatement("INSERT INTO pings (user, object, origin) VALUES (?, ?, ?)");
            insertStatement.setString(1, user);
            insertStatement.setString(2, articleId);
            insertStatement.setString(3, String.valueOf(Telifie.epochTime()));
            insertStatement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}