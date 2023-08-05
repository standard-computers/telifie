package com.telifie.Models.Connectors;

import com.sendgrid.Method;
import com.sendgrid.Request;
import com.sendgrid.Response;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;

import java.io.IOException;

public class SendGrid {

    private static final com.sendgrid.SendGrid sg = new com.sendgrid.SendGrid("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");;

    public static boolean sendCode(String email, String string){
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
                System.out.println("Failed to send email. Status code: " + response.getStatusCode());
                System.out.println(response.getBody());
                return false;
            }
        } catch (IOException e) {
            return false;
        }
    }
}
