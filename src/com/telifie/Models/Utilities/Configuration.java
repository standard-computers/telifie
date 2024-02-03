package com.telifie.Models.Utilities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import java.util.ArrayList;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Configuration {

    public static String SERVER_NAME = "";
    public static String VERSION = "v1.0.0b";
    private String email;
    private String mongodb;
    private ArrayList<String> iplist;
    public static Connection mysql;
    @JsonIgnore
    public static MongoClient mongoClient;
    private String license;

    public static String getServerName() {
        return SERVER_NAME;
    }

    public void setServer_name(String server_name) {
        this.SERVER_NAME = server_name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setMongodb(String mongodb) {
        this.mongodb = mongodb;
    }

    public String getMongodb() {
        return mongodb;
    }

    public void startMongo(){
        mongoClient = MongoClients.create(getMongodb());
    }

    public Connection getMysql() {
        return mysql;
    }

    public void setMysql(Connection mysql) {
        this.mysql = mysql;
    }

    public ArrayList<String> getIplist() {
        return iplist;
    }

    public void setIplist(ArrayList<String> ip_list) {
        this.iplist = ip_list;
    }

    public String getLicense() {
        return license;
    }

    public void setLicense(String license) {
        this.license = license;
    }

    public static class Connection {

        private String uri, user, psswd;

        public Connection() {
        }

        public Connection(String uri, String user, String psswd) {
            this.uri = uri;
            this.user = user;
            this.psswd = psswd;
        }

        public String getUri() {
            return uri;
        }

        public void setUri(String uri) {
            this.uri = uri;
        }

        public String getUser() {
            return user;
        }

        public void setUser(String user) {
            this.user = user;
        }

        public String getPsswd() {
            return psswd;
        }

        public void setPsswd(String psswd) {
            this.psswd = psswd;
        }
    }
}