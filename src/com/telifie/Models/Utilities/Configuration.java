package com.telifie.Models.Utilities;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Configuration {

    private static String VERSION = "v1.0.0b", mongodb, mysql;
    public static String model;
    @JsonIgnore
    public static MongoClient mongoClient;
    @JsonIgnore
    public static Connection sqlClient;

    @JsonCreator
    public Configuration(@JsonProperty("version") String VERSION, @JsonProperty("model") String model, @JsonProperty("mongodb") String mongodb, @JsonProperty("mysql") String mysql) {
        Configuration.VERSION = VERSION;
        Configuration.model = model;
        Configuration.mongodb = mongodb;
        Configuration.mysql = mysql;
    }

    @JsonIgnore
    public void connectDB(){
        mongoClient = MongoClients.create(mongodb);
        try {
            Connection connection = DriverManager.getConnection(mysql);
            System.out.println("SQL CONNECTED");
            sqlClient = connection;
        } catch (SQLException e) {
            Log.error("SQL DATABASE FAILED", "CLIx015");
            System.exit(-1);
        }
    }
}