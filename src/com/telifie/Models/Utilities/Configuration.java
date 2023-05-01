package com.telifie.Models.Utilities;

import com.telifie.Models.Domain;
import com.telifie.Models.User;
import java.io.Serializable;

public class Configuration implements Serializable {

    public final String VERSION = "1.0.0";
    private User user;
    private Authentication authentication = null;
    private String license = null;
    private Domain domain;

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Authentication getAuthentication() {
        return authentication;
    }

    public void setAuthentication(Authentication authentication) {
        this.authentication = authentication;
    }
    
    public void setLicense(String license) {
        this.license = license;
    }

    public Domain getDomain() {
        return domain;
    }

    public void setDomain(Domain domain) {
        this.domain = domain;
    }

    public boolean save(String systemDir){
        String dir = systemDir + "/telifie.configuration";
        Telifie.files.serialized(dir, this);
        return true;
    }

    @Override
    public String toString() {
        return "{\"user\" : \"" + user + "\"" +
                ", \"authentication\" : \"" + authentication + "\"" +
                ", \"license\" : \"" + license + "\"" +
                ", \"domain\" : \"" + domain + "\"}";
    }
}
