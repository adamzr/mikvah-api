package org.lamikvah.website.resource;

import java.util.Optional;

import org.lamikvah.website.MikvahConfiguration;
import org.lamikvah.website.dao.ProcessedStripeEventRespository;
import org.lamikvah.website.data.ProcessedStripeEvent;
import org.lamikvah.website.service.EmailService;
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
import com.stripe.model.StripeObject;
import com.stripe.model.Subscription;
import com.stripe.net.Webhook;

import lombok.extern.slf4j.Slf4j;

@RestController
@Slf4j
public class StripeWebhookController {

    @Autowired
    private MikvahConfiguration config;

    @Autowired
    private ProcessedStripeEventRespository repo;

    @Autowired
    private MembershipService membershipService;

    @Autowired
    private EmailService emailService;

    @PostMapping("/webhook")
    public ResponseEntity<String> handleStripeEvent(@RequestBody final String payload,
            @RequestHeader("Stripe-Signature") final String sigHeader) {

        log.info("Processing event {}", payload);
        Event event;
        try {
            event = Webhook.constructEvent(payload, sigHeader, config.getStripe().getWebhookEndpointSecret());
        } catch (final SignatureVerificationException e) {
            log.error("Signature validation failed!!!!", e);
            return ResponseEntity.noContent().build();
        }
        final String eventId = event.getId();
        final Optional<ProcessedStripeEvent> processedEvent = repo.findById(eventId);
        if (processedEvent.isPresent()) {
            log.warn("Already processed this event!");
            return ResponseEntity.noContent().build();
        }

        final String eventType = event.getType();
        final Optional<StripeObject> optionalObject = event.getDataObjectDeserializer().getObject();
        if (optionalObject.isEmpty()) {
            log.warn("Received event that could not be deserielized!!! event={}", event);
            return ResponseEntity.noContent().build();
        }
        final StripeObject object = optionalObject.get();
        switch (eventType) {
            case "invoice.upcoming":
                final Invoice upcomingInvoice = (Invoice) object;
                log.info("Sending email about upcoming membership renewal: {}", upcomingInvoice);
                emailService.sendUpcomingRenewalEmail(upcomingInvoice);
                break;
            case "invoice.payment_succeeded":
                final Invoice invoice = (Invoice) object;
                membershipService.updateMembersip(invoice);
                break;
            case "customer.subscription.deleted":
                final Subscription subscription = (Subscription) object;
                membershipService.cancelMembership(subscription);
                break;
            default:
                log.debug("Received event of type={}", eventType);
        }

        return ResponseEntity.noContent().build();

    }
}
