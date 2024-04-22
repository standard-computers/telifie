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
import java.util.ArrayList;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Configuration {

    private String VERSION = "v1.0.0b", email, mongodb, mysql;
    private ArrayList<String> iplist;
    @JsonIgnore
    public static MongoClient mongoClient;
    @JsonIgnore
    public static Connection mysqlClient;

    @JsonCreator
    public Configuration(@JsonProperty("version") String VERSION, @JsonProperty("email") String email, @JsonProperty("mongodb") String mongodb, @JsonProperty("mysql") String mysql, @JsonProperty("iplist") ArrayList<String> iplist) {
        this.VERSION = VERSION;
        this.email = email;
        this.mongodb = mongodb;
        this.mysql = mysql;
        this.iplist = iplist;
    }

    @JsonIgnore
    public void startMongo(){
        mongoClient = MongoClients.create(this.mongodb);
    }

    @JsonIgnore
    public void startSql(){
        try {
            Connection connection = DriverManager.getConnection(mysql);
            System.out.println("SQL CONNECTED");
            mysqlClient = connection;
        } catch (SQLException e) {
            Log.error("SQL DATABASE FAILED", "CLIx015");
            System.exit(-1);
        }
    }
}