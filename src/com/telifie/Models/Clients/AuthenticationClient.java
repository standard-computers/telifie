package com.telifie.Models.Clients;

import com.telifie.Models.Actions.Out;
import com.telifie.Models.Authentication;
import com.telifie.Models.Domain;
import org.bson.Document;

import java.util.Arrays;

public class AuthenticationClient extends Client {

    public AuthenticationClient(Domain domain) {
        super(domain);
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
        }else{
            return found.hasToken(authentication.getToken())
                    && found.hasRefreshToken(authentication.getRefreshToken());
        }
    }

}
