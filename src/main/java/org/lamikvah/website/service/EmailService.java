package org.lamikvah.website.service;

import java.io.StringWriter;
import java.text.NumberFormat;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.lamikvah.website.MikvahConfiguration;
import org.lamikvah.website.data.AppointmentSlot;
import org.lamikvah.website.data.CreditCard;
import org.lamikvah.website.data.Membership;
import org.lamikvah.website.data.MikvahUser;
import org.simplejavamail.email.Email;
import org.simplejavamail.email.EmailBuilder;
import org.simplejavamail.mailer.Mailer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import com.stripe.model.Invoice;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class EmailService {

    private static final int CENTS_PER_DOLLAR = 100;

    private static final DateTimeFormatter FRIENDLY_FORMAT = DateTimeFormatter.ofPattern("EEEE, MMMM, d, yyyy 'at' h:mm a");

    @Autowired private Mailer mailer;
    @Autowired private MikvahConfiguration config;
    @Autowired private CreditCardService creditCardService;

    private MustacheFactory mf = new DefaultMustacheFactory();
    private NumberFormat currencyFormatter = NumberFormat.getCurrencyInstance();

    private final LoadingCache<String, Mustache> MUSTACHE_CACHE = Caffeine.newBuilder()
            .maximumSize(100)
            .build(templateFileName -> mf.compile(templateFileName));

    @Async
    public void sendAppointmentConfirmationEmail(MikvahUser user, AppointmentSlot appointment) {

        try {

            Mustache htmlMustache = MUSTACHE_CACHE.get("emails/appointment-confirmation.html.mustache");

            Mustache txtMustache = MUSTACHE_CACHE.get("emails/appointment-confirmation.txt.mustache");

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
            context.put("amount", currencyFormatter.format(config.getAppointmentCost() / CENTS_PER_DOLLAR));

            sendEmail(user, htmlMustache, txtMustache, context, "You're Appointment Is Confirmed!");

        } catch (Exception e) {
            log.error("There was a problem sending the appointment confirmation email.", e);
        }
    }

    @Async
    public void sendAppointmentCancellationEmail(MikvahUser user, AppointmentSlot appointment, Optional<String> refundId) {

        try {

            Mustache htmlMustache = MUSTACHE_CACHE.get("emails/appointment-cancellation.html.mustache");

            Mustache txtMustache = MUSTACHE_CACHE.get("emails/appointment-cancellation.txt.mustache");

            Map<String, Object> context = new HashMap<>();
            context.put("appointmentSlotId", appointment.getId());
            context.put("fullName", user.getFullName());
            context.put("isoStartTime", appointment.getStart().atZone(ZoneId.of(config.getTimeZone())).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
            context.put("startDateTime", appointment.getStart().atZone(ZoneId.of(config.getTimeZone())).format(FRIENDLY_FORMAT));
            context.put("ccRefunded", refundId.isPresent());
            context.put("confirmationCode", refundId.orElse(null));
            context.put("amount", currencyFormatter.format(config.getAppointmentCost() / CENTS_PER_DOLLAR));

            sendEmail(user, htmlMustache, txtMustache, context, "You're Appointment Has Been Cancelled");

        } catch (Exception e) {
            log.error("There was a problem sending the appointment cancellation email.", e);
        }
    }

    @Async
    public void sendWelcomeEmail(MikvahUser user) {

        try {

            Mustache htmlMustache = MUSTACHE_CACHE.get("emails/welcome.html.mustache");

            Mustache txtMustache = MUSTACHE_CACHE.get("emails/welcome.txt.mustache");

            Map<String, Object> context = new HashMap<>();

            sendEmail(user, htmlMustache, txtMustache, context, "Welcome to the Los Angeles Mikvah Society - Mikvat Esteher Website");

        } catch (Exception e) {
            log.error("There was a problem sending the appointment cancellation email.", e);
        }
    }

    @Async
    public void sendNewMemberEmail(MikvahUser user, Membership membership) {
        try {

            Mustache htmlMustache = MUSTACHE_CACHE.get("emails/new-membership.html.mustache");

            Mustache txtMustache = MUSTACHE_CACHE.get("emails/new-membership.txt.mustache");

            Map<String, Object> context = new HashMap<>();
            context.put("level", membership.getPlan().name());
            context.put("expirationDate", membership.getExpiration().format(FRIENDLY_FORMAT));
            Optional<CreditCard> creditCard = creditCardService.getCreditCard(user);
            if(creditCard.isPresent()) {
                context.put("last4", creditCard.get().getLast4());
            } else {
                context.put("last4", "XXXX");
            }
            context.put("autoRenewEnabled", membership.isAutoRenewEnabled());

            sendEmail(user, htmlMustache, txtMustache, context, "Mikvah Membership");

        } catch (Exception e) {

            log.error("There was a problem sending the mikvah membership start email.", e);

        }

    }

    @Async
    public void sendMembershipRenewalEmail(MikvahUser user, Membership membership) {
        try {

            Mustache htmlMustache = MUSTACHE_CACHE.get("emails/membership-renewal.html.mustache");

            Mustache txtMustache = MUSTACHE_CACHE.get("emails/membership-renewal.txt.mustache");

            Map<String, Object> context = new HashMap<>();
            context.put("level", membership.getPlan().name());
            context.put("expirationDate", membership.getExpiration().format(FRIENDLY_FORMAT));
            Optional<CreditCard> creditCard = creditCardService.getCreditCard(user);
            if(creditCard.isPresent()) {
                context.put("last4", creditCard.get().getLast4());
            } else {
                context.put("last4", "XXXX");
            }
            context.put("autoRenewEnabled", membership.isAutoRenewEnabled());

            sendEmail(user, htmlMustache, txtMustache, context, "Mikvah Membership");

        } catch (Exception e) {

            log.error("There was a problem sending the mikvah membership renewal email.", e);

        }

    }

    @Async
    public void sendCreditCardUpdateEmail(MikvahUser user, Optional<CreditCard> creditCard) {
        // TODO Auto-generated method stub

    }

    @Async
    public void sendAutoRenewDisabledEmail(MikvahUser user) {
        // TODO Auto-generated method stub

    }

    @Async
    public void sendAutoRenewEnabledEmail(MikvahUser user) {
        // TODO Auto-generated method stub

    }

    @Async
    public void sendUpcomingRenewalEmail(Invoice upcomingInvoice) {
        // TODO Auto-generated method stub
    }

    @Async
    public void sendMembershipEndedEmail(MikvahUser user) {
        // TODO Auto-generated method stub

    }

    @Async
    public void sendDonationEmail(MikvahUser user) {
        // TODO Auto-generated method stub

    }

    private void sendEmail(
            MikvahUser user,
            Mustache htmlMustache,
            Mustache txtMustache,
            Map<String, Object> context,
            String subject
            ) {

        StringWriter htmlWriter = new StringWriter();
        StringWriter txtWriter = new StringWriter();

        htmlMustache.execute(htmlWriter, context);
        txtMustache.execute(txtWriter, context);

        Email email = EmailBuilder
         .startingBlank()
        .from("Los Angeles Mikvah Society", "mikvah@mikvah.email")
        .to(user.getFullName(), user.getEmail())
        .withSubject(subject)
        .withPlainText(txtWriter.toString())
        .withHTMLText(htmlWriter.toString())
        .buildEmail();

        mailer.sendMail(email, true);
    }


}
