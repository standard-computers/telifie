package com.telifie.Models.Utilities;

import com.telifie.Models.Clients.Client;
import com.telifie.Models.User;
import org.bson.Document;
import java.util.Arrays;

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

    public String getUser() {
        return user;
    }

    public boolean hasToken(String token){
        return token.equals(this.token);
    }

    public boolean hasRefreshToken(String token){
        return token.equals(this.refresh);
    }

    public boolean authenticate(){
        return new AuthenticationClient().authenticate(this);
    }

    public boolean isAuthenticated(){
        return new AuthenticationClient().isAuthenticated(this);
    }

    @Override
    public String toString() {
        return new StringBuilder().append("{").append("\"user\" : \"").append(user).append('\"').append(", \"token\" : \"").append(token).append('\"').append(", \"refresh\" : \"").append(refresh).append('\"').append(", \"origin\" : ").append(origin).append(", \"expiration\" : ").append(expiration).append("}").toString();
    }

    private class AuthenticationClient extends Client {

        public AuthenticationClient() {
            super(null);
            super.collection = "authentications";
        }

        public boolean authenticate(Authentication authentication){
            return super.insertOne(Document.parse(authentication.toString()));
        }

        public boolean isAuthenticated(Authentication authentication){
            Document findAuthentication = this.findOne(new Document("$and", Arrays.asList(new Document("user", authentication.user), new Document("token", authentication.token))));
            if(findAuthentication == null){
                return false;
            }
            Authentication found = new Authentication(findAuthentication);
            int epoch = (int) (System.currentTimeMillis() / 1000);
            if(epoch > found.expiration){
                return false;
            }
            return found.hasToken(authentication.token) && found.hasRefreshToken(authentication.refresh);
        }
    }
}