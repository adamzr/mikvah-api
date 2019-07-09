package org.lamikvah.website.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.ActiveProfiles;

import com.stripe.model.Charge;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class DonationServiceTest {

    @Autowired
    private DonationService donationService;

    @Test
    public void testAnonymousDonation() throws Exception {

        final Charge charge = donationService.donate(null, "", "donationtest" + UUID.randomUUID() + "@mikvahtest.xyz",
                18, "tok_visa");
        assertEquals(1800l, charge.getAmount().longValue());
        assertNotNull(charge.getId());

    }

}
