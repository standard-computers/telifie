package com.telifie.Models.Utilities;

public class Session {

    private String user, domain; //User ID, Domain ID

    public Session(String user, String domain) {
        this.user = user;
        this.domain = domain;
    }

    public String getUser() {
        return user;
    }

    public String getDomain() {
        return domain;
    }
}
