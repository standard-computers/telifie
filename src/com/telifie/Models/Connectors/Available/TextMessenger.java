package com.telifie.Models.Connectors.Available;

import com.twilio.Twilio;
import com.twilio.base.ResourceSet;
import com.twilio.rest.api.v2010.account.Message;

public class TextMessenger {

    // Find your Account Sid and Token at twilio.com/console
    public static final String ACCOUNT_SID = "AC62612d034fff5be14392544b53d119c3";
    public static final String AUTH_TOKEN = "ccdce9e5a7d3abef9dc2af662346b999";

    public void receive(){
        Twilio.init(ACCOUNT_SID, AUTH_TOKEN);
        ResourceSet<Message> message = Message.reader().read();
    }

    public void send(String to, String from, String content){
        Twilio.init(ACCOUNT_SID, AUTH_TOKEN);
        Message message = Message.creator(
                new com.twilio.type.PhoneNumber(to),
                new com.twilio.type.PhoneNumber(from),
                content).create();
        System.out.println(message.getSid());
    }

}
