package org.lamikvah.website;

import javax.validation.constraints.NotNull;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import lombok.Data;

@ConfigurationProperties("mikvah")
@Component
@Data
@Validated
public class MikvahConfiguration {

    @NotNull
    private Auth0Properties auth0;

    @NotNull
    private StripeProperties stripe;

    private int appointmentCost;

    private String currency;

    private String timeZone;

    private String mikvahTreasurerEmail = "adamzr+liz@gmail.com";//"liz@edmunds.com";

    private String fromEmailAddress = "mikvah@mikvah.email";

    @Data
    public static class Auth0Properties {

        @NotNull
        private String managementToken;

        @NotNull
        private String issuer;

        @NotNull
        private String apiAudience;
    }

    @Data
    public static class StripeProperties {

        @NotNull
        private String apiSecret;

        private String webhookEndpointSecret;

    }
}
