package org.lamikvah.website.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.lamikvah.website.dao.MembershipRepository;
import org.lamikvah.website.dao.MikvahUserRepository;
import org.lamikvah.website.data.Membership;
import org.lamikvah.website.data.MikvahUser;
import org.lamikvah.website.data.Plan;
import org.lamikvah.website.exception.ServerErrorException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.stripe.exception.APIConnectionException;
import com.stripe.exception.APIException;
import com.stripe.exception.AuthenticationException;
import com.stripe.exception.CardException;
import com.stripe.exception.InvalidRequestException;
import com.stripe.model.Invoice;
import com.stripe.model.InvoiceLineItem;
import com.stripe.model.Subscription;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class MembershipService {

    @Autowired private MembershipRepository membershipRepository;
    @Autowired private MikvahUserRepository userRepository;

    private static final String ERROR_MESSAGE = "There was a problem handling your membership payment. Please try again later.";

    public void createMembership(MikvahUser user, Plan plan) {
        Map<String, Object> item = new HashMap<>();
        item.put("plan", plan.getStripePlanId());

        Map<String, Object> items = new HashMap<>();
        items.put("0", item);

        Map<String, Object> params = new HashMap<>();
        params.put("customer", user.getStripeCustomerId());
        params.put("items", items);
        try {
            Subscription.create(params);
        } catch (AuthenticationException e) {
            log.error("STRIPE: Failure to properly authenticate yourself in the request.", e);
            throw new ServerErrorException(ERROR_MESSAGE, e);
        } catch (InvalidRequestException e) {
            log.error("STRIPE: Invalid request errors arise when your request has invalid parameters.", e);
            throw new ServerErrorException(ERROR_MESSAGE, e);
        } catch (APIConnectionException e) {
            log.error("STRIPE: Failure to connect to Stripe's API.", e);
            throw new ServerErrorException(ERROR_MESSAGE, e);
        } catch (CardException e) {
            log.warn("STRIPE: Failure with the card.", e);
            throw new ServerErrorException(e.getLocalizedMessage(), e);
        } catch (APIException e) {
            log.warn("STRIPE: API errors cover any other type of problem (e.g., a temporary problem with Stripe's servers) and are extremely uncommon.", e);
            throw new ServerErrorException(ERROR_MESSAGE, e);
        }

        log.info("User with id={} signed up for membership with plan={}", user.getId(), plan);

    }

    public void updateMembersip(Invoice invoice) {

        if(invoice.getPaid()) {

            Iterable<InvoiceLineItem> invoiceLines = invoice.getLines().autoPagingIterable();
            for(InvoiceLineItem line: invoiceLines) {

                if(line.getType().equalsIgnoreCase("subscription")) {

                    String subscriptionId = line.getId();
                    Optional<Membership> membershipOptional = membershipRepository.findByStripeSubscriptionId(subscriptionId);
                    Membership membership;

                    if(membershipOptional.isPresent()) {

                        membership = membershipOptional.get();

                    } else {

                        String customerId = invoice.getCustomer();
                        Optional<MikvahUser> customer = userRepository.findByStripeCustomerId(customerId);
                        if(!customer.isPresent()) {
                            log.error("Received event for unknown customer!");
                            return;
                        }
                        Optional<Plan> plan = Plan.forStripePlanName(line.getPlan().getId());
                        if(!plan.isPresent()) {
                            log.error("Unknown plan={}!", line.getPlan().getId());
                            return;
                        }

                        membership = Membership.builder()
                                .mikvahUser(customer.get())
                                .plan(plan.get())
                                .start(LocalDateTime.now(Clock.systemUTC()))
                                .stripeSubscriptionId(subscriptionId)
                                .autoRenewEnabled(true)
                                .build();

                    }

                    LocalDateTime expiration = LocalDateTime.ofEpochSecond(line.getPeriod().getEnd(), 0, ZoneOffset.UTC);
                    membership.setExpiration(expiration);
                    membershipRepository.save(membership);

                    MikvahUser user = membership.getMikvahUser();
                    user.setMember(true);
                    userRepository.save(user);

                    log.info("Activated membership for user with id={} based on invoice={}", user.getId(), invoice.getId());

                }

            }

        }

    }

    public void cancelMembership(Subscription subscription) {

        Optional<Membership> membership = membershipRepository.findByStripeSubscriptionId(subscription.getId());
        if(!membership.isPresent()) {
            log.error("Missing membership for subscription={}", subscription);
            return;
        }
        membershipRepository.delete(membership.get());

        Optional<MikvahUser> userOptional = userRepository.findByStripeCustomerId(subscription.getCustomer());
        if(!userOptional.isPresent()) {
            log.error("Missing user for subscription={}", subscription);
            return;
        }
        MikvahUser user = userOptional.get();
        user.setMember(false);
        userRepository.save(user);

        log.info("Cancelled membership for user with id={} subscription={}.", user.getId(), subscription.getId());
        return;

    }

    public void disableAutoRenew(MikvahUser user) throws AuthenticationException, InvalidRequestException, APIConnectionException, CardException, APIException {
        Optional<Membership> membershipOptional = membershipRepository.findByMikvahUser(user);
        if(membershipOptional.isPresent()) {
            Membership membership = membershipOptional.get();
            if(membership.isAutoRenewEnabled()) {
                Subscription subscription = Subscription.retrieve(membership.getStripeSubscriptionId());
                Map<String, Object> params = new HashMap<>();
                params.put("at_period_end", true);
                subscription.cancel(params);
                membership.setAutoRenewEnabled(false);
                membershipRepository.save(membership);
            }
        }
    }

    public void enableAutoRenew(MikvahUser user) throws AuthenticationException, InvalidRequestException, APIConnectionException, CardException, APIException {
        Optional<Membership> membershipOptional = membershipRepository.findByMikvahUser(user);
        if(membershipOptional.isPresent()) {
            Membership membership = membershipOptional.get();
            if(!membership.isAutoRenewEnabled()) {
                Subscription subscription = Subscription.retrieve(membership.getStripeSubscriptionId());

                Map<String, Object> item = new HashMap<>();
                item.put("id", subscription.getSubscriptionItems().getData().get(0).getId());
                item.put("plan", membership.getPlan().getStripePlanId());

                Map<String, Object> items = new HashMap<>();
                items.put("0", item);

                Map<String, Object> params = new HashMap<>();
                params.put("items", items);

                subscription.update(params);

                membership.setAutoRenewEnabled(true);
                membershipRepository.save(membership);
            }
        }
    }

}
