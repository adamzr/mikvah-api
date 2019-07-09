package org.lamikvah.website.service;

import java.math.BigDecimal;

import org.lamikvah.website.data.MikvahUser;
import org.lamikvah.website.exception.DonationPaymentException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.stripe.exception.ApiConnectionException;
import com.stripe.exception.ApiException;
import com.stripe.exception.AuthenticationException;
import com.stripe.exception.CardException;
import com.stripe.exception.InvalidRequestException;
import com.stripe.exception.StripeException;
import com.stripe.model.Charge;
import com.stripe.param.ChargeCreateParams;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class DonationService {

    @Autowired
    private EmailService emailService;

    public Charge donate(final MikvahUser user, final String name, final String email, final double amount,
            final String stripeToken) {

        // Charge the user's card:
        final ChargeCreateParams.Builder chargeParamsBuilder = ChargeCreateParams.builder()
                .setAmount(getAmountInCents(amount)).setCurrency("usd")
                .setStatementDescriptor("Donation");
        if (user != null) {
            chargeParamsBuilder.setCustomer(user.getStripeCustomerId());
        } else {
            chargeParamsBuilder.setSource(stripeToken);
        }
        final Charge charge;
        try {
            charge = Charge.create(chargeParamsBuilder.build());
            log.info("User with id={} email={} donated amount={} USD with charge ID={}",
                    user != null ? user.getId() : "N/A", email, amount, charge.getId());
        } catch (final CardException e) {
            log.info("Card processing error.", e);
            throw new DonationPaymentException(
                    "There was a problem processing your payment. " + e.getLocalizedMessage());
        } catch (AuthenticationException | InvalidRequestException | ApiConnectionException | ApiException e) {
            log.error("Payment processing error.", e);
            throw new DonationPaymentException("There was a problem processing your payment. Please try again later.",
                    e);
        } catch (final StripeException e) {
            log.error("Payment processing error.", e);
            throw new DonationPaymentException("There was a problem processing your payment. Please try again later.",
                    e);
        }

        final String to = user != null ? user.getFullName() : name;
        final String emailAddress = user != null ? user.getEmail() : email;
        emailService.sendDonationEmail(to, emailAddress, amount);
        return charge;

    }

    private long getAmountInCents(final double amount) {

        return new BigDecimal(amount).multiply(new BigDecimal(100)).longValue();
    }

}
