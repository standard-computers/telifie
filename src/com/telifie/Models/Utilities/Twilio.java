package com.telifie.Models.Utilities;

import com.telifie.Models.Clients.Packages;
import com.twilio.rest.api.v2010.account.Message;

public class Twilio {

    public static final String ACCOUNT_SID = Packages.get("com.telifie.connectors.twilio").getAccess();
    public static final String AUTH_TOKEN = Packages.get("com.telifie.connectors.twilio").getSecret();

    public static void send(String to, String from, String content){
        com.twilio.Twilio.init(ACCOUNT_SID, AUTH_TOKEN);
        Message.creator(new com.twilio.type.PhoneNumber(to), new com.twilio.type.PhoneNumber(from), content).create();
    }

    public static boolean sendCode(String email, String code){
        return false;
    }
}