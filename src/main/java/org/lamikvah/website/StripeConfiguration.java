package org.lamikvah.website;

import javax.annotation.PostConstruct;

import org.springframework.stereotype.Component;

import com.stripe.Stripe;

@Component
public class StripeConfiguration {
    
    @PostConstruct
    public void setupStripe() {
        Stripe.apiKey = System.getenv("STRIPE_SECRET");
    }
    
}
