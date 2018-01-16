package org.lamikvah.website.service;

import java.util.HashMap;
import java.util.Map;

import org.lamikvah.website.data.MikvahUser;
import org.lamikvah.website.exception.AppointmentCreationException;
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

    public void donate(MikvahUser user, double amount) {

        // Charge the user's card:
        Map<String, Object> chargeParams = new HashMap<>();
        chargeParams.put("amount", String.valueOf((int)(amount * 100)));
        chargeParams.put("currency", "usd");
        chargeParams.put("customer", user.getStripeCustomerId());
        try {
            Charge charge = Charge.create(chargeParams);
            log.info("User with id={} donated amount={} USD with charge ID={}", user.getId(), amount, charge.getId());
        } catch (AuthenticationException | InvalidRequestException | APIConnectionException | APIException e) {
            log.error("Payment processing error.", e);
            throw new AppointmentCreationException("There was a problem processing your payment. Please try again later.");
        }  catch (CardException e) {
            log.info("Card processing error.", e);
            throw new AppointmentCreationException("There was a problem processing your payment. " + e.getLocalizedMessage());
        }

    }

}
