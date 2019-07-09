package org.lamikvah.website.service;

import java.util.Optional;

import org.lamikvah.website.data.CreditCard;
import org.lamikvah.website.data.Membership;
import org.lamikvah.website.data.MikvahUser;
import org.lamikvah.website.exception.ServerErrorException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.stripe.exception.ApiConnectionException;
import com.stripe.exception.ApiException;
import com.stripe.exception.AuthenticationException;
import com.stripe.exception.CardException;
import com.stripe.exception.InvalidRequestException;
import com.stripe.exception.StripeException;
import com.stripe.model.Card;
import com.stripe.model.Customer;
import com.stripe.model.PaymentSource;
import com.stripe.param.CustomerCreateParams;
import com.stripe.param.CustomerUpdateParams;
import com.stripe.param.PaymentSourceCollectionCreateParams;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class CreditCardService {

    private static final String ERROR_MESSAGE = "There was a problem processing your payment information. Please try again later.";

    @Autowired
    private MikvahUserService mikvahUserService;

    @Autowired
    @Lazy
    private Optional<EmailService> emailService;

    @Autowired
    private MembershipService membershipService;

    public void addNewCreditCard(final MikvahUser user, final String token) {

        boolean isUsersFirstCreditCard = false;

        try {
            if (StringUtils.isEmpty(user.getStripeCustomerId())) {
                isUsersFirstCreditCard = true;
                createCustomer(user);
            }
            createCard(user, token);
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

        if (!isUsersFirstCreditCard) {
            final Optional<Membership> membership = membershipService.getMembership(user);
            emailService.get().sendCreditCardUpdateEmail(user, membership);
        }

    }

    public Optional<CreditCard> getCreditCard(final MikvahUser user) {

        if (StringUtils.isEmpty(user.getStripeCustomerId()))
            return Optional.empty();
        try {
            final Customer customer = Customer.retrieve(user.getStripeCustomerId());
            return customer.getSources().getData().stream()
                    .filter(source -> source.getId().equals(customer.getDefaultSource())).map(externalAccount -> {
                        final Card card = (Card) externalAccount;
                        return CreditCard.builder().brand(card.getBrand())
                                .expirationMonth(card.getExpMonth().intValue())
                                .expirationYear(card.getExpYear().intValue()).last4(card.getLast4()).build();
                    }).findFirst();

        } catch (final StripeException e) {
            log.error("There was an error getting the credit card information.", e);
            return Optional.empty();
        }
    }

    private void createCard(final MikvahUser user, final String token) throws AuthenticationException,
            InvalidRequestException, ApiConnectionException, CardException, ApiException, StripeException {

        final String customerId = user.getStripeCustomerId();
        final Customer customer = Customer.retrieve(customerId);
        final PaymentSourceCollectionCreateParams params = PaymentSourceCollectionCreateParams.builder()
                .setSource(token).build();
        final PaymentSource source = customer.getSources().create(params);
        setDefaultCard(customerId, source.getId());

    }

    private void setDefaultCard(final String customerId, final String sourceId) throws AuthenticationException,
            InvalidRequestException, ApiConnectionException, CardException, ApiException, StripeException {

        final Customer customer = Customer.retrieve(customerId);
        final CustomerUpdateParams params = CustomerUpdateParams.builder().setDefaultSource(sourceId).build();
        customer.update(params);

    }

    private void createCustomer(final MikvahUser user) throws AuthenticationException, InvalidRequestException,
            ApiConnectionException, CardException, ApiException, StripeException {

        final CustomerCreateParams params = CustomerCreateParams.builder().setDescription(user.getFullName())
                .setEmail(user.getEmail()).putMetadata("mikvah_user_id", String.valueOf(user.getId())).build();

        final Customer customer = Customer.create(params);
        user.setStripeCustomerId(customer.getId());
        mikvahUserService.saveUser(user);

    }

}
