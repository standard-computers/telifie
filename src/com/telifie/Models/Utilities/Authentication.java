package com.telifie.Models.Utilities;

import com.telifie.Models.User;
import org.bson.Document;
import org.json.JSONObject;

public class Authentication {

    private final String user, token, refreshToken;
    private int origin, expiration;

    public Authentication(String user) {
        this.user = user;
        this.token = Telifie.tools.make.md5(Telifie.tools.make.randomReferenceCode());
        this.refreshToken = Telifie.tools.make.md5(Telifie.tools.make.randomReferenceCode());
        this.origin = (int) (System.currentTimeMillis() / 1000);
        this.expiration = this.origin + 2419000; //28 Days until expiration
    }

    public Authentication(String[] bearer){
        this.user = bearer[0];
        this.token = bearer[1];
        this.refreshToken = bearer[2];
    }

    public Authentication(Document document) throws NullPointerException {
        this.user = document.getString("user");
        this.token = document.getString("token");
        this.refreshToken = document.getString("refresh_token");
        this.origin = document.getInteger("origin");
        this.expiration = document.getInteger("expiration");
    }

    public Authentication(User user){
        this(user.getId());
    }

    public Authentication(String user, String token, String refreshToken) {
        this.user = user;
        this.token = token;
        this.refreshToken = refreshToken;
        //TODO Get information from authentication client (origin, expiration)
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

    public int getOrigin() {
        return origin;
    }

    public int getExpiration() {
        return expiration;
    }

    public void refreshToken(){
        //TODO Refresh token with database and AuthenticationClient
    }

    public boolean hasToken(String token){
        if(token.equals(this.token)){
            return true;
        }
        return false;
    }

    public boolean hasRefreshToken(String token){
        if(token.equals(this.refreshToken)){
            return true;
        }
        return false;
    }

    @Override
    public String toString() {
        return "{" +
                "\"user\" : \"" + user + '\"' +
                ", \"token\" : \"" + token + '\"' +
                ", \"refresh_token\" : \"" + refreshToken + '\"' +
                ", \"origin\" : " + origin +
                ", \"expiration\" : " + expiration +
                '}';
    }

    public JSONObject toJson(){
        return new JSONObject(this.toString());
    }

    public Document document(){
        return Document.parse(this.toString());
    }

}
