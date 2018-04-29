package org.lamikvah.website;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.stripe.Stripe;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class StripeConfiguration {

    public StripeConfiguration(@Autowired MikvahConfiguration config) {

        log.info("Initializing Stripe...");
        Stripe.apiKey = config.getStripe().getApiSecret();

    }

}
