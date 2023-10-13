package com.telifie.Models.Utilities;

public class Session {

    private final String user;
    private String domain;

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

    public void setDomain(String domain) {
        this.domain = domain;
    }
}
