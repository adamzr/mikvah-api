package org.lamikvah.website.service;

import java.util.HashMap;
import java.util.Map;

import org.lamikvah.website.data.MikvahUser;
import org.lamikvah.website.exception.DonationPaymentException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.stripe.exception.APIConnectionException;
import com.stripe.exception.APIException;
import com.stripe.exception.AuthenticationException;
import com.stripe.exception.CardException;
import com.stripe.exception.InvalidRequestException;
import com.stripe.model.Charge;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class DonationService {

    @Autowired private EmailService emailService;

    public void donate(MikvahUser user, String name, String email, double amount, String stripeToken) {

        // Charge the user's card:
        Map<String, Object> chargeParams = new HashMap<>();
        chargeParams.put("amount", String.valueOf((int)(amount * 100)));
        chargeParams.put("currency", "usd");
        if (user != null) {
            chargeParams.put("customer", user.getStripeCustomerId());
        } else {
            chargeParams.put("source", stripeToken);
        }
        try {
            Charge charge = Charge.create(chargeParams);
            log.info("User with id={} email={} donated amount={} USD with charge ID={}", user != null ? user.getId() : "N/A", email, amount, charge.getId());
        } catch (AuthenticationException | InvalidRequestException | APIConnectionException | APIException e) {
            log.error("Payment processing error.", e);
            throw new DonationPaymentException("There was a problem processing your payment. Please try again later.", e);
        }  catch (CardException e) {
            log.info("Card processing error.", e);
            throw new DonationPaymentException("There was a problem processing your payment. " + e.getLocalizedMessage());
        }

        String to = user != null ? user.getFullName() : name;
        String emailAddress = user != null ? user.getEmail() : email;
        emailService.sendDonationEmail(to, emailAddress, amount);

    }

}
