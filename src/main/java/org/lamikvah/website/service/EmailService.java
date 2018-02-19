package org.lamikvah.website.service;

import java.io.StringWriter;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.lamikvah.website.MikvahConfiguration;
import org.lamikvah.website.data.AppointmentSlot;
import org.lamikvah.website.data.CreditCard;
import org.lamikvah.website.data.MikvahUser;
import org.simplejavamail.email.Email;
import org.simplejavamail.email.EmailBuilder;
import org.simplejavamail.mailer.Mailer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class EmailService {

    private static final DateTimeFormatter FRIENDLY_FORMAT = DateTimeFormatter.ofPattern("EEEE, MMMM, d, yyyy 'at' h:mm a");

    @Autowired private Mailer mailer;
    @Autowired private MikvahConfiguration config;
    @Autowired private CreditCardService creditCardService;
    private MustacheFactory mf = new DefaultMustacheFactory();

    public void sendAppointmentConfirmationEmail(MikvahUser user, AppointmentSlot appointment) {

        try {
            StringWriter htmlWriter = new StringWriter();
            Mustache htmlMustache = mf.compile("emails/appointment-confirmation.html.mustache");

            StringWriter txtWriter = new StringWriter();
            Mustache txtMustache = mf.compile("emails/appointment-confirmation.txt.mustache");

            Map<String, Object> context = new HashMap<>();
            context.put("appointmentSlotId", appointment.getId());
            context.put("fullName", user.getFullName());
            context.put("isoStartTime", appointment.getStart().atZone(ZoneId.of(config.getTimeZone())).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
            context.put("startDateTime", appointment.getStart().atZone(ZoneId.of(config.getTimeZone())).format(FRIENDLY_FORMAT));
            context.put("ccCharged", !StringUtils.isEmpty(appointment.getStripeChargeId()));
            Optional<CreditCard> creditCardOptional = creditCardService.getCreditCard(user);
            if(creditCardOptional.isPresent()) {
                CreditCard creditCard = creditCardOptional.get();
                context.put("cardType", creditCard.getBrand());
                context.put("last4", creditCard.getLast4());
            }
            context.put("confirmationCode", appointment.getStripeChargeId());
            context.put("amount", config.getAppointmentCost());

            htmlMustache.execute(htmlWriter, context);
            txtMustache.execute(txtWriter, context);

            Email email = EmailBuilder
             .startingBlank()
            .from("Los Angeles Mikvah Society", "appointments@mikvah.email")
            .to(user.getFullName(), user.getEmail())
            .withSubject("You're Appointment Is Confirmed!")
            .withPlainText(txtWriter.toString())
            .withHTMLText(htmlWriter.toString())
            .buildEmail();

            mailer.sendMail(email, true);
        } catch (Exception e) {
            log.error("There was a problem sending the appointment confirmation email.", e);
        }
    }
}
