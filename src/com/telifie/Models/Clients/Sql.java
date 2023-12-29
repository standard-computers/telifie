package com.telifie.Models.Clients;

import com.telifie.Models.Utilities.Configuration;
import com.telifie.Models.Utilities.Console;
import com.telifie.Models.Utilities.Log;
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
        try {
            PreparedStatement command = this.sql.prepareStatement("INSERT INTO parsed (user, uri, origin) VALUES (?, ?, ?)");
            command.setString(1, user);
            command.setString(2, url);
            command.setString(3, String.valueOf(Telifie.epochTime()));
            command.execute();
            this.sql.close();
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
                this.sql.close();
                return count > 0;
            }
            return false;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void deleteParsed(String uri) {
        try (PreparedStatement command = this.sql.prepareStatement("DELETE FROM parsed WHERE uri = ?")) {
            command.setString(1, uri);
            command.execute();
            this.sql.close();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void purgeQueue() {
        try {
            this.sql.prepareStatement("TRUNCATE queue").execute();
            this.sql.close();
            Log.message("QUEUE PURGED", "SQLx067");
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
            this.sql.close();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean isQueued(String url) {
        try {
            PreparedStatement command = this.sql.prepareStatement("SELECT COUNT(*) AS count FROM queue WHERE uri = ?");
            command.setString(1, url);
            ResultSet resultSet = command.executeQuery();
            if (resultSet.next()) {
                int count = resultSet.getInt("count");
                command.close();
                return count > 0;
            }
            return false;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void deleteQueued(String uri) {
        try (PreparedStatement command = this.sql.prepareStatement("DELETE FROM queue WHERE uri = ?")) {
            command.setString(1, uri);
            command.execute();
            this.sql.close();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public String nextQueued() {
        String uri = null;
        try (PreparedStatement selectCommand = this.sql.prepareStatement("SELECT uri FROM queue ORDER BY origin DESC LIMIT 1")) {
            try (ResultSet resultSet = selectCommand.executeQuery()) {
                if (resultSet.next()) {
                    uri = resultSet.getString("uri");
                    deleteQueued(uri);
                    this.sql.close();
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return uri;
    }

}