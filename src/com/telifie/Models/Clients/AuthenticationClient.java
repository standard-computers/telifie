package com.telifie.Models.Clients;

import com.telifie.Models.Utilities.Authentication;
import com.telifie.Models.Utilities.Configuration;
import org.bson.Document;

import java.util.Arrays;

public class AuthenticationClient extends Client {

    public AuthenticationClient(Configuration config) {

        super(config);
        super.collection = "authentications";
    }

    //TODO
    public boolean authenticate(Authentication authentication){

        return super.insertOne(authentication.document());

    }

    public boolean isAuthenticated(Authentication authentication){

        Document findAuthentication = this.findOne(
                new Document("$and", Arrays.asList(
                        new Document("user", authentication.getUser()),
                        new Document("token", authentication.getToken())
                    )
                ));

        if(findAuthentication == null){
            return false;
        }
        Authentication found = new Authentication(findAuthentication);
        int epoch = (int) (System.currentTimeMillis() / 1000);
        if(epoch > found.getExpiration()){
            return false;
        }
        return found.hasToken(authentication.getToken())
                && found.hasRefreshToken(authentication.getRefreshToken());
    }

}
