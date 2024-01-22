package com.telifie.Models.Utilities;

import com.telifie.Models.User;
import org.bson.Document;

public class Authentication {

    private final String user, token, refresh;
    private int origin, expiration;

    public Authentication(String bearer){
        String[] b = bearer.split(" ")[1].split("\\.");
        this.user = b[0];
        this.token = b[1];
        this.refresh = b[2];
    }

    public Authentication(Document document) throws NullPointerException {
        this.user = document.getString("user");
        this.token = document.getString("token");
        this.refresh = document.getString("refresh");
        this.origin = document.getInteger("origin");
        this.expiration = document.getInteger("expiration");
    }

    public Authentication(User user){
        this.user = user.getId();
        this.token = Telifie.md5(Telifie.randomReferenceCode());
        this.refresh = Telifie.md5(Telifie.randomReferenceCode());
        this.origin = Telifie.epochTime();
        this.expiration = this.origin + 2419000;
    }

    public Authentication(String user, int access){
        this.user = user;
        this.token = Telifie.md5(Telifie.randomReferenceCode());
        this.refresh = Telifie.md5(Telifie.randomReferenceCode());
        this.origin = Telifie.epochTime();
        this.expiration = this.origin + 2419000;
    }

    public String getUser() {
        return user;
    }

    public String getToken() {
        return token;
    }

    public String getRefresh() {
        return refresh;
    }

    public int getExpiration() {
        return expiration;
    }

    public boolean hasToken(String token){
        return token.equals(this.token);
    }

    public boolean hasRefreshToken(String token){
        return token.equals(this.refresh);
    }

    @Override
    public String toString() {
        return new StringBuilder().append("{").append("\"user\" : \"").append(user).append('\"').append(", \"token\" : \"").append(token).append('\"').append(", \"refresh\" : \"").append(refresh).append('\"').append(", \"origin\" : ").append(origin).append(", \"expiration\" : ").append(expiration).append("}").toString();
    }
}