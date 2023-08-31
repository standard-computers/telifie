package com.telifie.Models.Utilities;

import com.telifie.Models.User;
import org.bson.Document;
import org.json.JSONObject;

public class Authentication {

    private final String user, token, refreshToken;
    private int origin, expiration;

    public Authentication(String bearer){
        String[] b = bearer.split(" ")[1].split("\\.");
        this.user = b[0];
        this.token = b[1];
        this.refreshToken = b[2];
    }

    public Authentication(Document document) throws NullPointerException {
        this.user = document.getString("user");
        this.token = document.getString("token");
        this.refreshToken = document.getString("refresh_token");
        this.origin = document.getInteger("origin");
        this.expiration = document.getInteger("expiration");
    }

    public Authentication(User user){
        this.user = user.getId();
        this.token = Telifie.tools.make.md5(Telifie.tools.make.randomReferenceCode());
        this.refreshToken = Telifie.tools.make.md5(Telifie.tools.make.randomReferenceCode());
        this.origin = (int) (System.currentTimeMillis() / 1000);
        this.expiration = this.origin + 2419000; //28 Days until expiration
    }

    public String getUser() {
        return user;
    }

    public String getToken() {
        return token;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public int getExpiration() {
        return expiration;
    }

    public boolean hasToken(String token){
        return token.equals(this.token);
    }

    public boolean hasRefreshToken(String token){
        return token.equals(this.refreshToken);
    }

    @Override
    public String toString() {
        return new StringBuilder().append("{")
                .append("\"user\" : \"").append(user).append('\"')
                .append(", \"token\" : \"").append(token).append('\"')
                .append(", \"refresh_token\" : \"").append(refreshToken).append('\"')
                .append(", \"origin\" : ").append(origin)
                .append(", \"expiration\" : ").append(expiration)
                .append("}").toString();
    }

    public JSONObject toJson(){
        return new JSONObject(this.toString());
    }

    public Document document(){
        return Document.parse(this.toString());
    }

}
