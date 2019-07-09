package org.lamikvah.website.service;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.lamikvah.website.MikvahConfiguration;
import org.lamikvah.website.data.Membership;
import org.lamikvah.website.data.MikvahUser;
import org.lamikvah.website.data.Plan;
import org.lamikvah.website.data.UserCreationRequestDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

import com.stripe.model.Subscription;
import com.stripe.model.WebhookEndpoint;
import com.stripe.param.WebhookEndpointCreateParams;
import com.stripe.param.WebhookEndpointCreateParams.ApiVersion;
import com.stripe.param.WebhookEndpointCreateParams.EnabledEvent;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class MembershipServiceTest {

    @LocalServerPort
    private int port;

    @Autowired
    private MembershipService membershipService;

    @Autowired
    private MikvahUserService mikvahUserService;

    @Autowired
    private CreditCardService creditCardService;

    @Autowired
    private MikvahConfiguration config;

    @Test
    public void testCreateMembership() throws Exception {

        final String random = UUID.randomUUID().toString();

        final ProcessBuilder pb = new ProcessBuilder("./ngrok", "http", "-subdomain=adamzr" + random, "" + port);
        pb.directory(new File("/Users/aricheimer"));
        pb.inheritIO();
        final Process ngrokProcess = pb.start();

        WebhookEndpoint webhookEndpoint;
        try {
            final WebhookEndpointCreateParams params = WebhookEndpointCreateParams.builder()
                    .setUrl("https://adamzr" + random + ".ngrok.io/webhook")
                    .addEnabledEvent(EnabledEvent.INVOICE__PAYMENT_SUCCEEDED)
                    .setApiVersion(ApiVersion.VERSION_2019_05_16)
                    .build();
            webhookEndpoint = WebhookEndpoint.create(params);
            config.getStripe().setWebhookEndpointSecret(webhookEndpoint.getSecret());

            final UserCreationRequestDto request = new UserCreationRequestDto();
            request.setAddressLine1("1 Main St");
            request.setAddressLine2("Apt 1");
            request.setCity("Los Angeles");
            request.setCountryCode("US");
            request.setEmail("testmembership" + random + "@mikvahtest.xyz");
            request.setFirstName("First" + random);
            request.setLastName("Last" + random);
            request.setPhoneNumber("+13105551234");
            request.setPostalCode("90035");
            request.setStateCode("CA");
            request.setTitle("Mrs.");
            final MikvahUser mikvahUser = mikvahUserService.createUser(request);

            creditCardService.addNewCreditCard(mikvahUser, "tok_visa");

            membershipService.createMembership(mikvahUser, Plan.STANDARD);

            Thread.sleep(10000);

            Optional<Membership> optionalMembership = membershipService.getMembership(mikvahUser);
            Membership membership = optionalMembership.orElse(null);
            assertNotNull(membership);
            assertTrue(membership.getMikvahUser().isMember());

            assertTrue(membership.isAutoRenewEnabled());

            membershipService.disableAutoRenew(mikvahUser);
            optionalMembership = membershipService.getMembership(mikvahUser);
            membership = optionalMembership.orElse(null);
            assertFalse(membership.isAutoRenewEnabled());
            Subscription subscription = Subscription.retrieve(membership.getStripeSubscriptionId());
            assertTrue(subscription.getCancelAtPeriodEnd());

            membershipService.enableAutoRenew(mikvahUser);
            optionalMembership = membershipService.getMembership(mikvahUser);
            membership = optionalMembership.orElse(null);
            assertTrue(membership.isAutoRenewEnabled());
            subscription = Subscription.retrieve(membership.getStripeSubscriptionId());
            assertFalse(subscription.getCancelAtPeriodEnd());

        } catch (final Throwable t) {
            ngrokProcess.destroyForcibly();
            throw t;
        }

        ngrokProcess.destroy();

        webhookEndpoint.delete();

    }

}
