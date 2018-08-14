package org.lamikvah.website.service;

import java.security.Principal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

import javax.servlet.http.HttpServletRequest;

import org.lamikvah.website.MikvahConfiguration;
import org.lamikvah.website.dao.AppointmentSlotRepository;
import org.lamikvah.website.dao.MembershipRepository;
import org.lamikvah.website.dao.MikvahUserRepository;
import org.lamikvah.website.data.AppointmentSlot;
import org.lamikvah.website.data.AppointmentSlotDto;
import org.lamikvah.website.data.CreditCard;
import org.lamikvah.website.data.DailyHours;
import org.lamikvah.website.data.Membership;
import org.lamikvah.website.data.MikvahUser;
import org.lamikvah.website.data.Plan;
import org.lamikvah.website.data.UserCreationRequestDto;
import org.lamikvah.website.data.UserDto;
import org.lamikvah.website.data.UserRequestDto;
import org.lamikvah.website.exception.ServerErrorException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import com.auth0.client.mgmt.ManagementAPI;
import com.auth0.client.mgmt.filter.UserFilter;
import com.auth0.exception.Auth0Exception;
import com.auth0.json.mgmt.users.User;
import com.auth0.net.Request;

@Component
public class MikvahUserService {

    @Autowired private MikvahConfiguration config;
    @Autowired private MikvahUserRepository userRepository;
    @Autowired private CreditCardService creditCardService;
    @Autowired private AppointmentSlotRepository appointmentSlotRepository;
    @Autowired private MembershipRepository membershipRepository;
    @Autowired @Lazy private Optional<EmailService> emailService;
    @Autowired private DailyHoursService dailyHoursService;

    private final ManagementAPI auth0ManagementApi;

    private static final UserFilter NO_OP_USER_FILTER = new UserFilter();

    @Autowired
    public MikvahUserService(@Autowired MikvahConfiguration config) {
        auth0ManagementApi = new ManagementAPI(config.getAuth0().getIssuer(), config.getAuth0().getManagementToken());
    }

    private String getUserEmail(String principalName) throws Auth0Exception {

        Request<User> apiRequest = auth0ManagementApi.users().get(principalName, NO_OP_USER_FILTER);
        User user = apiRequest.execute();
        return user.getEmail();

    }

    public MikvahUser saveUser(String auth0UserId, UserRequestDto request) throws Auth0Exception {

        MikvahUser user = userRepository.getByAuth0UserId(auth0UserId).orElse(new MikvahUser());
        if(user.getEmail() == null) {
            String email = getUserEmail(auth0UserId);
            user.setEmail(email);
        }
        user.setTitle(request.getTitle());
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setAuth0UserId(auth0UserId);

        user.setAddressLine1(request.getAddressLine1());
        user.setAddressLine2(request.getAddressLine2());
        user.setCity(request.getCity());
        user.setStateCode(request.getStateCode());
        user.setCountryCode(request.getCountryCode());
        user.setNotes(request.getNotes());
        user.setPhoneNumber(request.getPhoneNumber());
        user.setPostalCode(request.getPostalCode());

        return userRepository.save(user);

    }

    public MikvahUser getUser(HttpServletRequest request){

        Principal principal = request.getUserPrincipal();
        String auth0UserId =  principal.getName();
        try {
            return getUser(auth0UserId);
        } catch (Auth0Exception e) {
            throw new ServerErrorException("There was a problem getting your user information. Please try again later.",
                    e);
        }

    }

    public UserDto getUserWithCreditCardInfo(String auth0UserId) throws Auth0Exception {
        MikvahUser user = getUser(auth0UserId);
        return convertToUserDto(user);
    }

    public MikvahUser getUser(String auth0UserId) throws Auth0Exception {

        Optional<MikvahUser> user = userRepository.getByAuth0UserId(auth0UserId);
        if(!user.isPresent()) {
            String email = getUserEmail(auth0UserId);
            user = userRepository.findByEmail(email);
            if(!user.isPresent()) {
                MikvahUser newUser = new MikvahUser();
                newUser.setAuth0UserId(auth0UserId);
                newUser.setEmail(email);
                emailService.get().sendWelcomeEmail(newUser);
                return userRepository.save(newUser);
            } else {
                MikvahUser partialUser = user.get();
                partialUser.setAuth0UserId(auth0UserId);
                return userRepository.save(partialUser);
            }

        }
        return user.get();

    }

    public void saveUser(MikvahUser user) {

        userRepository.save(user);

    }

    private UserDto convertToUserDto(MikvahUser user) {

        Optional<CreditCard> card = creditCardService.getCreditCard(user);
        CreditCard defaultCard = card.orElse(null);

        AppointmentSlotDto currentAppointment = null;
        LocalDateTime now = LocalDateTime.now(Clock.system(ZoneId.of(config.getTimeZone())));
        LocalDateTime thirtyDaysFromNow = now.plusDays(30);
        List<AppointmentSlot> appointments = appointmentSlotRepository.findByStartBetweenAndMikvahUserOrderByStartAsc(now, thirtyDaysFromNow, user);
        if(!CollectionUtils.isEmpty(appointments)) {
            AppointmentSlot appointment = appointments.get(appointments.size() - 1);
            
            LocalDate appointmentDay = appointment.getStart().toLocalDate();
            Optional<DailyHours> hours = dailyHoursService.getHoursForDay(appointmentDay);
            LocalTime openingTime = hours.get().getOpeningLocalTime().get();
            ZonedDateTime lastCancellation = LocalDateTime.of(appointmentDay, openingTime).atZone(ZoneId.of(config.getTimeZone()));
            
            currentAppointment = AppointmentSlotDto.builder()
                    .id(appointment.getId())
                    .start(appointment.getStart())
                    .build();
            currentAppointment.setLastCancellation(lastCancellation);
        }

        Plan plan = null;
        LocalDateTime expirationDate = null;
        boolean membershipAutoRenewalEnabled = true;
        Optional<Membership> membership = membershipRepository.findByMikvahUser(user);
        if(membership.isPresent()) {
            plan = membership.get().getPlan();
            expirationDate = membership.get().getExpiration();
            membershipAutoRenewalEnabled = membership.get().isAutoRenewEnabled();
        }

        return UserDto.builder()
                .auth0UserId(user.getAuth0UserId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .id(user.getId())
                .lastName(user.getLastName())
                .addressLine1(user.getAddressLine1())
                .addressLine2(user.getAddressLine2())
                .city(user.getCity())
                .postalCode(user.getPostalCode())
                .countryCode(user.getCountryCode())
                .phoneNumber(user.getPhoneNumber())
                .notes(user.getNotes())
                .member(user.isMember())
                .stripeCustomerId(user.getStripeCustomerId())
                .title(user.getTitle())
                .defaultCard(defaultCard)
                .currentAppointment(currentAppointment)
                .membershipExpirationDate(expirationDate)
                .membershipPlan(plan)
                .membershipAutoRenewalEnabled(membershipAutoRenewalEnabled)
                .build();
    }

    public MikvahUser createUser(UserCreationRequestDto request) {

        String email = request.getEmail();
        Optional<MikvahUser> existingUser = userRepository.findByEmail(email);
        if(existingUser.isPresent()) {
            throw new IllegalArgumentException("A user with that email already exists.");
        }
        MikvahUser user = new MikvahUser();
        user.setEmail(email);
        user.setTitle(request.getTitle());
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setAddressLine1(request.getAddressLine1());
        user.setAddressLine2(request.getAddressLine2());
        user.setCity(request.getCity());
        user.setStateCode(request.getStateCode());
        user.setCountryCode(request.getCountryCode());
        user.setNotes(request.getNotes());
        user.setPhoneNumber(request.getPhoneNumber());
        user.setPostalCode(request.getPostalCode());

        return userRepository.save(user);
    }
}
