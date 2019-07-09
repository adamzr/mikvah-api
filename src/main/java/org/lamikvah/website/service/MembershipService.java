package org.lamikvah.website.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.lamikvah.website.dao.MembershipRepository;
import org.lamikvah.website.dao.MikvahUserRepository;
import org.lamikvah.website.data.Membership;
import org.lamikvah.website.data.MikvahUser;
import org.lamikvah.website.data.Plan;
import org.lamikvah.website.exception.AlreadyMemberException;
import org.lamikvah.website.exception.ServerErrorException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.stripe.exception.ApiConnectionException;
import com.stripe.exception.ApiException;
import com.stripe.exception.AuthenticationException;
import com.stripe.exception.CardException;
import com.stripe.exception.InvalidRequestException;
import com.stripe.exception.StripeException;
import com.stripe.model.Invoice;
import com.stripe.model.InvoiceLineItem;
import com.stripe.model.Subscription;
import com.stripe.param.SubscriptionCreateParams;
import com.stripe.param.SubscriptionCreateParams.Item;
import com.stripe.param.SubscriptionUpdateParams;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class MembershipService {

    private static final int NONE = 0;

    private static final int TWENTY_FOUR_HOURS = 86400000;

    private static final int MEMBERSHIP_LENGTH = 1;

    @Autowired
    private MembershipRepository membershipRepository;

    @Autowired
    private MikvahUserRepository userRepository;

    @Autowired
    @Lazy
    private Optional<EmailService> emailService;

    private static final String ERROR_MESSAGE = "There was a problem handling your membership payment. Please try again later.";

    public Optional<Membership> getMembership(final MikvahUser user) {

        return membershipRepository.findByMikvahUser(user);

    }

    public Membership createOfflineMembership(final MikvahUser user, final Plan plan) {

        final LocalDateTime start = LocalDateTime.now(Clock.systemUTC());
        final LocalDateTime expiration = start.plusYears(MEMBERSHIP_LENGTH);
        final Membership membership = Membership.builder()
                .mikvahUser(user)
                .plan(plan)
                .start(start)
                .expiration(expiration)
                .autoRenewEnabled(false)
                .build();

        return membershipRepository.save(membership);

    }

    public void createMembership(final MikvahUser user, final Plan plan) {

        if (user.isMember()) {
            log.warn("User {} attempting to subscribe at level {} is already a member.", user, plan);
            throw new AlreadyMemberException();
        }
        final SubscriptionCreateParams subscriptionCreateParams = SubscriptionCreateParams.builder()
                .addItem(Item.builder().setPlan(plan.getStripePlanId()).build())
                .setCustomer(user.getStripeCustomerId())
                .build();
        try {
            Subscription.create(subscriptionCreateParams);
        } catch (final AuthenticationException e) {
            log.error("STRIPE: Failure to properly authenticate yourself in the request.", e);
            throw new ServerErrorException(ERROR_MESSAGE, e);
        } catch (final InvalidRequestException e) {
            log.error("STRIPE: Invalid request errors arise when your request has invalid parameters.", e);
            throw new ServerErrorException(ERROR_MESSAGE, e);
        } catch (final ApiConnectionException e) {
            log.error("STRIPE: Failure to connect to Stripe's API.", e);
            throw new ServerErrorException(ERROR_MESSAGE, e);
        } catch (final CardException e) {
            log.warn("STRIPE: Failure with the card.", e);
            throw new ServerErrorException(e.getLocalizedMessage(), e);
        } catch (final ApiException e) {
            log.warn(
                    "STRIPE: API errors cover any other type of problem (e.g., a temporary problem with Stripe's servers) and are extremely uncommon.",
                    e);
            throw new ServerErrorException(ERROR_MESSAGE, e);
        } catch (final StripeException e) {
            log.warn(
                    "STRIPE: Stripe errors cover any other type of problem (e.g., a temporary problem with Stripe's servers) and are extremely uncommon.",
                    e);
            throw new ServerErrorException(ERROR_MESSAGE, e);
        }

        log.info("User with id={} signed up for membership with plan={}", user.getId(), plan);

    }

    public void updateMembersip(final Invoice invoice) {

        if (invoice.getPaid()) {

            final Iterable<InvoiceLineItem> invoiceLines = invoice.getLines().autoPagingIterable();
            for (final InvoiceLineItem line : invoiceLines) {

                if (line.getType().equalsIgnoreCase("subscription")) {

                    final String subscriptionId = line.getSubscription();
                    final Optional<Membership> membershipForSubscriptionOptional = membershipRepository
                            .findByStripeSubscriptionId(subscriptionId);
                    Membership membership;
                    boolean isNewMembership = false;

                    if (membershipForSubscriptionOptional.isPresent()) {
                        // This is a renewal of an existing membership
                        membership = membershipForSubscriptionOptional.get();
                        isNewMembership = false;

                    } else {

                        final String customerId = invoice.getCustomer();
                        final Optional<MikvahUser> customer = userRepository.findByStripeCustomerId(customerId);
                        if (!customer.isPresent()) {
                            log.error("Received event for unknown customer!");
                            return;
                        }
                        final Optional<Plan> plan = Plan.forStripePlanName(line.getPlan().getId());
                        if (!plan.isPresent()) {
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

                    final LocalDateTime expiration = LocalDateTime.ofEpochSecond(line.getPeriod().getEnd(), 0,
                            ZoneOffset.UTC);
                    membership.setExpiration(expiration);
                    membershipRepository.save(membership);

                    final MikvahUser user = membership.getMikvahUser();
                    user.setMember(true);
                    userRepository.save(user);

                    if (isNewMembership) {
                        emailService.get().sendNewMemberEmail(user, membership);
                    } else {
                        emailService.get().sendMembershipRenewalEmail(user, membership);
                    }

                    log.info("Activated membership for user with id={} based on invoice={}", user.getId(),
                            invoice.getId());

                }

            }

        }

    }

    public void cancelMembership(final Subscription subscription) {

        final Optional<Membership> membership = membershipRepository.findByStripeSubscriptionId(subscription.getId());
        if (!membership.isPresent()) {
            log.error("Missing membership for subscription={}", subscription);
            return;
        }
        membershipRepository.delete(membership.get());

        final Optional<MikvahUser> userOptional = userRepository.findByStripeCustomerId(subscription.getCustomer());
        if (!userOptional.isPresent()) {
            log.error("Missing user for subscription={}", subscription);
            return;
        }
        final MikvahUser user = userOptional.get();
        user.setMember(false);
        userRepository.save(user);

        emailService.get().sendMembershipEndedEmail(user);

        log.info("Cancelled membership for user with id={} subscription={}.", user.getId(), subscription.getId());

    }

    @Scheduled(initialDelay = NONE, fixedRate = TWENTY_FOUR_HOURS)
    public void cancelOfflineSubscriptions() {

        log.info("Checking for non-Stripe managed memberships...");

        final LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);
        final List<Membership> membershipsToCancel = new ArrayList<>();

        for (final Membership membership : membershipRepository.findAll()) {
            if (!membership.isAutoRenewEnabled()
                    && membership.getExpiration().isBefore(sevenDaysAgo)) {
                log.info("Canceling membership {}", membership);
                membershipsToCancel.add(membership);
                final MikvahUser user = membership.getMikvahUser();
                if (user != null) {
                    emailService.get().sendMembershipEndedEmail(membership.getMikvahUser());
                    membershipsToCancel.add(membership);
                    user.setMember(false);
                    userRepository.save(user);
                }
            }
        }

        membershipRepository.deleteAll(membershipsToCancel);

    }

    public void disableAutoRenew(final MikvahUser user) throws StripeException {

        final Optional<Membership> membershipOptional = membershipRepository.findByMikvahUser(user);
        if (membershipOptional.isPresent()) {
            final Membership membership = membershipOptional.get();
            if (membership.isAutoRenewEnabled()) {
                final Subscription subscription = Subscription.retrieve(membership.getStripeSubscriptionId());
                final SubscriptionUpdateParams params = SubscriptionUpdateParams.builder()
                        .setCancelAtPeriodEnd(true)
                        .build();
                subscription.update(params);
                membership.setAutoRenewEnabled(false);
                membershipRepository.save(membership);
                emailService.get().sendAutoRenewDisabledEmail(user, membership);
                log.info("Disabled auto-renew for {}", user);
            }
        }
    }

    public void enableAutoRenew(final MikvahUser user) throws StripeException {

        final Optional<Membership> membershipOptional = membershipRepository.findByMikvahUser(user);
        if (membershipOptional.isPresent()) {
            final Membership membership = membershipOptional.get();
            if (!membership.isAutoRenewEnabled()) {
                final Subscription subscription = Subscription.retrieve(membership.getStripeSubscriptionId());

                final SubscriptionUpdateParams params = SubscriptionUpdateParams.builder()
                        .setCancelAtPeriodEnd(false)
                        .build();
                subscription.update(params);

                membership.setAutoRenewEnabled(true);
                membershipRepository.save(membership);

                emailService.get().sendAutoRenewEnabledEmail(user, membership);
                log.info("Enabled auto-renew for {}", user);

            }
        }
    }

}
