package com.telifie.Models.Utilities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import java.util.ArrayList;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Configuration {

    private String installation = "REMOTE", email;
    private String mongodb; //JSON of database configuration
    private ArrayList<String> ip_list; //List of IP to externally connect
    private ArrayList<String> ip_access; //List of IP to allowed to access server
    private ArrayList<String> ip_block; //List of IP to allowed to access server

    public static Connection file_storage, mysql;
    public static MongoClient mongoClient;
    private String license;

    public String getInstallation() {
        return installation;
    }

    public void setInstallation(String installation) {
        this.installation = installation;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getURI() {
        return mongodb;
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

    public MongoClient getClient(){
        return mongoClient;
    }

    public Connection getFile_storage() {
        return file_storage;
    }

    public void setFile_storage(Connection file_storage) {
        this.file_storage = file_storage;
    }

    public Connection getMysql() {
        return mysql;
    }

    public void setMysql(Connection mysql) {
        this.mysql = mysql;
    }

    public ArrayList<String> getIp_list() {
        return ip_list;
    }

    public void setIp_list(ArrayList<String> ip_list) {
        this.ip_list = ip_list;
    }

    public ArrayList<String> getIp_access() {
        return ip_access;
    }

    public void setIp_access(ArrayList<String> ip_access) {
        this.ip_access = ip_access;
    }

    public ArrayList<String> getIp_block() {
        return ip_block;
    }

    public void setIp_block(ArrayList<String> ip_block) {
        this.ip_block = ip_block;
    }

    public String getLicense() {
        return license;
    }

    public void setLicense(String license) {
        this.license = license;
    }

    @Override
    public String toString() {
        return "Configuration{" +
                "installation='" + installation + '\'' +
                ", email='" + email + '\'' +
                ", mongodb='" + mongodb + '\'' +
                ", file_storage=" + file_storage +
                ", mysql=" + mysql +
                ", ip_list=" + ip_list +
                ", ip_access=" + ip_access +
                ", ip_block=" + ip_block +
                ", license='" + license + '\'' +
                '}';
    }

    public class Connection {

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
