package com.telifie.Models.Utilities;

import com.telifie.Models.Actions.Out;
import com.telifie.Models.Authentication;
import com.telifie.Models.Domain;
import com.telifie.Models.User;
import java.io.Serializable;
import java.util.ArrayList;

public class Configuration implements Serializable {

    public final String VERSION = "1.0.0";
    private User user;
    private Authentication authentication = null;
    private String license = null;
    private ArrayList<Domain> domains = new ArrayList<>();

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

    public String getLicense() {
        return license;
    }

    public void setLicense(String license) {
        this.license = license;
    }

    public Domain defaultDomain(){
        return this.domains.get(0);
    }

    public void setDefaultDomain(Domain domain){
        this.domains.add(0, domain);
    }

    public void addDomain(Domain domain){
        this.domains.add(domain);
    }

    public Domain getDomain(int index){
        return this.domains.get(index);
    }

    public boolean save(String systemDir){
        String dir = systemDir + "/telifie.configuration";
        Out.serialized(dir, this);
        return true;
    }

    @Override
    public String toString() {
        return "{" +
                "user : " + user +
                ", authentication : " + authentication +
                ", license : '" + license + '\'' +
                ", domains : " + domains +
                '}';
    }
}
