package com.telifie.Models.Connectors;

import com.telifie.Models.Utilities.Packages;
import com.twilio.rest.api.v2010.account.Message;

public class Twilio {

//    public static final String ACCOUNT_SID = "AC62612d034fff5be14392544b53d119c3";
    public static final String ACCOUNT_SID = Packages.get("com.telifie.connectors.twilio").getAccess();
//    public static final String AUTH_TOKEN = "ccdce9e5a7d3abef9dc2af662346b999";
    public static final String AUTH_TOKEN = Packages.get("com.telifie.connectors.twilio").getSecret();;

    public static void send(String to, String from, String content){
        com.twilio.Twilio.init(ACCOUNT_SID, AUTH_TOKEN);
        Message.creator(
                new com.twilio.type.PhoneNumber(to),
                new com.twilio.type.PhoneNumber(from),
                content).create();
    }

}
