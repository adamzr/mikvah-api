package org.lamikvah.website.service;

import com.auth0.exception.Auth0Exception;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.lamikvah.website.MikvahConfiguration;
import org.lamikvah.website.dao.AppointmentSlotRepository;
import org.lamikvah.website.dao.MembershipRepository;
import org.lamikvah.website.dao.MikvahUserRepository;
import org.lamikvah.website.data.MikvahUser;
import org.lamikvah.website.data.UserDto;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

class MikvahUserServiceTest {
    private static final String SomeAuth0UserId = "some-auth0-user-id";
    private static final String SomeZoneId = "America/Los_Angeles";

    @Mock private MikvahUserRepository userRepository;

    @Mock private CreditCardService creditCardService;

    @Mock private AppointmentSlotRepository appointmentSlotRepository;

    @Mock private MembershipRepository membershipRepository;

    @Mock private EmailService emailService;

    @Mock private DailyHoursService dailyHoursService;

    @ParameterizedTest
    @MethodSource("databaseValues")
    void shouldMapAdminProperty(boolean databaseValue) throws Auth0Exception {
        MockitoAnnotations.initMocks(this);

        when(userRepository.getByAuth0UserId(SomeAuth0UserId)).thenReturn(Optional.of(MikvahUser.builder().admin(databaseValue).build()));

        final MikvahConfiguration.Auth0Properties auth0Properties = new MikvahConfiguration.Auth0Properties();
        auth0Properties.setIssuer("some-issuer");
        auth0Properties.setManagementToken("some-token");

        final MikvahConfiguration config = new MikvahConfiguration();
        config.setAuth0(auth0Properties);
        config.setTimeZone(SomeZoneId);

        MikvahUserService subject = new MikvahUserService(config, userRepository, appointmentSlotRepository,
                membershipRepository, creditCardService, emailService, dailyHoursService);

        final UserDto user = subject.getUserWithCreditCardInfo(SomeAuth0UserId);

        assertThat(user.isAdmin()).isEqualTo(databaseValue);

    }

    private static Collection<Boolean> databaseValues() {
        return Arrays.asList(true, false);
    }
}
