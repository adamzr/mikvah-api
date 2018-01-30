package org.lamikvah.website.resource;

import org.lamikvah.website.MikvahConfiguration;
import org.lamikvah.website.dao.ProcessedStripeEventRespository;
import org.lamikvah.website.data.ProcessedStripeEvent;
import org.lamikvah.website.service.MembershipService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.Invoice;
import com.stripe.model.Subscription;
import com.stripe.net.Webhook;

import lombok.extern.slf4j.Slf4j;

@RestController
@Slf4j
public class StripeWebhookController {

    @Autowired private MikvahConfiguration config;
    @Autowired private ProcessedStripeEventRespository repo;
    @Autowired private MembershipService membershipService;

    @PostMapping("/webhook")
    public ResponseEntity<String> handleStripeEvent(@RequestBody String payload, @RequestHeader("Stripe-Signature") String sigHeader) {

        log.info("Processing event {}", payload);
        Event event;
        try {
            event = Webhook.constructEvent(payload, sigHeader, config.getStripe().getWebhookEndpointSecret());
        } catch (SignatureVerificationException e) {
            log.error("Signature validation failed!!!!", e);
            return ResponseEntity.noContent().build();
        }
        String eventId = event.getId();
        ProcessedStripeEvent processedEvent = repo.findOne(eventId);
        if(processedEvent != null) {
            log.warn("Already processed this event!");
            return ResponseEntity.noContent().build();
        }

        String eventType = event.getType();
        switch(eventType) {
            case "invoice.payment_succeeded":
                Invoice invoice = (Invoice) event.getData().getObject();
                membershipService.updateMembersip(invoice);
                break;
            case "customer.subscription.deleted":
                Subscription subscription = (Subscription) event.getData().getObject();
                membershipService.cancelMembership(subscription);
                break;
            default:
                log.debug("Received event of type={}", eventType);
        }

        return ResponseEntity.noContent().build();

    }
}
