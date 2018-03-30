package org.lamikvah.website.service;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.lamikvah.website.data.CreditCard;
import org.lamikvah.website.data.MikvahUser;
import org.lamikvah.website.exception.ServerErrorException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.stripe.exception.APIConnectionException;
import com.stripe.exception.APIException;
import com.stripe.exception.AuthenticationException;
import com.stripe.exception.CardException;
import com.stripe.exception.InvalidRequestException;
import com.stripe.model.Card;
import com.stripe.model.Customer;
import com.stripe.model.ExternalAccount;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class CreditCardService {

    private static final String ERROR_MESSAGE = "There was a problem processing your payment information. Please try again later.";
    @Autowired
    private MikvahUserService mikvahUserService;

    @Autowired
    private EmailService emailService;

    public void addNewCreditCard(MikvahUser user, String token) {

        boolean isUsersFirstCreditCard = false;

        try {
            if(StringUtils.isEmpty(user.getStripeCustomerId())) {
                isUsersFirstCreditCard = true;
                createCustomer(user);
            }
            createCard(user, token);
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

        if(!isUsersFirstCreditCard) {
            emailService.sendCreditCardUpdateEmail(user, getCreditCard(user));
        }
    }

    public Optional<CreditCard> getCreditCard(MikvahUser user) {
        if(StringUtils.isEmpty(user.getStripeCustomerId())) {
            return Optional.empty();
        }
        try {
            Customer customer = Customer.retrieve(user.getStripeCustomerId());
            return customer.getSources().getData()
            .stream()
            .filter(source -> source.getId().equals(customer.getDefaultSource()))
            .map(externalAccount -> {
                Card card = (Card) externalAccount;
                return CreditCard.builder()
                .brand(card.getBrand())
                .expirationMonth(card.getExpMonth())
                .expirationYear(card.getExpYear())
                .last4(card.getLast4())
                .build();
            })
            .findFirst();

        } catch (AuthenticationException | InvalidRequestException | APIConnectionException | CardException
                | APIException e) {
            log.error("There was an error getting the credit card information.", e);
            return Optional.empty();
        }
    }

    private void createCard(MikvahUser user, String token) throws AuthenticationException, InvalidRequestException, APIConnectionException, CardException, APIException {
        String customerId = user.getStripeCustomerId();
        Customer customer = Customer.retrieve(customerId);
        Map<String, Object> params = new HashMap<>();
        params.put("source", token);
        ExternalAccount source = customer.getSources().create(params);
        setDefaultCard(customerId, source.getId());
    }

    private void setDefaultCard(String customerId, String sourceId) throws AuthenticationException, InvalidRequestException, APIConnectionException, CardException, APIException {
        Customer customer = Customer.retrieve(customerId);
        Map<String, Object> params = new HashMap<>();
        params.put("default_source", sourceId);
        customer.update(params);
    }

    private void createCustomer(MikvahUser user) throws AuthenticationException, InvalidRequestException, APIConnectionException, CardException, APIException {
        Map<String, Object> customerParams = new HashMap<>();
        customerParams.put("description", (user.getTitle() + " " + user.getFirstName() + " " + user.getLastName()).trim());
        customerParams.put("email", user.getEmail());
        Customer customer = Customer.create(customerParams);
        user.setStripeCustomerId(customer.getId());
        mikvahUserService.saveUser(user);
    }

}
