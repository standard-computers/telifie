package com.telifie.Models.Connectors;

import com.sendgrid.Method;
import com.sendgrid.Request;
import com.sendgrid.Response;
import com.sendgrid.SendGrid;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;
import com.telifie.Models.Utilities.Telifie;

import java.io.IOException;

public class SGrid {

    private final SendGrid sg;

    public SGrid(Connector connector){

        sg = new SendGrid(connector.getSecret());
    }

    public boolean sendAuthenticationCode(String email, String string){

        Request req = new Request();
        Email from = new Email("no-reply@telifie.com");
        String subject = "Verify your email";
        Email to = new Email(email);
        Content content = new Content(
            "text/plain",
            "This is a message from Telifie to verify your email. Your email verification code is: " + string
        );
        Mail mail = new Mail(from, subject, to, content);

        try {

            req.setMethod(Method.POST);
            req.setEndpoint("mail/send");
            req.setBody(mail.build());
            Response response = sg.api(req);
            if (response.getStatusCode() == 202) {

                return true;
            } else {

                Telifie.console.out.string("Failed to send email. Status code: " + response.getStatusCode());
                Telifie.console.out.string(response.getBody());
                return false;
            }
        } catch (IOException e) {

            return false;
        }
    }
}
