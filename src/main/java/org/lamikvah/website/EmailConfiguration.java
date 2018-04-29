package org.lamikvah.website;

import org.simplejavamail.mailer.Mailer;
import org.simplejavamail.mailer.MailerBuilder;
import org.simplejavamail.mailer.config.TransportStrategy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EmailConfiguration {

    private static final int SMTP_PORT = 465;
    private static final String SMTP_SERVER = "email-smtp.us-west-2.amazonaws.com";

    @Bean
    public Mailer mailer(@Value("${SMTP_USERNAME}") String username, @Value("${SMTP_PASSWORD}") String password) {

        return MailerBuilder
        .withSMTPServer(SMTP_SERVER, SMTP_PORT)
        .withTransportStrategy(TransportStrategy.SMTPS)
        .withSMTPServerUsername(username)
        .withSMTPServerPassword(password)
        .buildMailer();

    }

}
