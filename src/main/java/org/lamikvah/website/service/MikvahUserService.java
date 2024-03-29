package org.lamikvah.website.service;

import com.auth0.client.mgmt.ManagementAPI;
import com.auth0.client.mgmt.filter.UserFilter;
import com.auth0.exception.Auth0Exception;
import com.auth0.json.mgmt.users.User;
import com.auth0.net.Request;
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

@Component
public class MikvahUserService {

  private final MikvahConfiguration config;

  private final MikvahUserRepository userRepository;

  private final CreditCardService creditCardService;

  private final AppointmentSlotRepository appointmentSlotRepository;

  private final MembershipRepository membershipRepository;

  private final EmailService emailService;

  private final DailyHoursService dailyHoursService;

  private final ManagementAPI auth0ManagementApi;

  private static final UserFilter NO_OP_USER_FILTER = new UserFilter();

  @Autowired
  public MikvahUserService(final MikvahConfiguration config,
      final MikvahUserRepository userRepository,
      final AppointmentSlotRepository appointmentSlotRepository,
      final MembershipRepository membershipRepository,
      @Lazy final CreditCardService creditCardService,
      @Lazy final EmailService emailService,
      final DailyHoursService dailyHoursService) {

    auth0ManagementApi =
        new ManagementAPI(config.getAuth0().getIssuer(), config.getAuth0().getManagementToken());
    this.config = config;
    this.userRepository = userRepository;
    this.appointmentSlotRepository = appointmentSlotRepository;
    this.membershipRepository = membershipRepository;
    this.creditCardService = creditCardService;
    this.emailService = emailService;
    this.dailyHoursService = dailyHoursService;
  }

  private String getUserEmail(final String principalName) throws Auth0Exception {

    final Request<User> apiRequest =
        auth0ManagementApi.users().get(principalName, NO_OP_USER_FILTER);
    final User user = apiRequest.execute();
    return user.getEmail();

  }

  public MikvahUser saveUser(final String auth0UserId, final UserRequestDto request)
      throws Auth0Exception {

    final MikvahUser user = userRepository.getByAuth0UserId(auth0UserId).orElse(new MikvahUser());
    if (user.getEmail() == null) {
      final String email = getUserEmail(auth0UserId);
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

  public MikvahUser getUser(final HttpServletRequest request) {

    final Principal principal = request.getUserPrincipal();
    final String auth0UserId = principal.getName();
    try {
      return getUser(auth0UserId);
    } catch (final Auth0Exception e) {
      throw new ServerErrorException(
          "There was a problem getting your user information. Please try again later.",
          e);
    }

  }

  public UserDto getUserWithCreditCardInfo(final String auth0UserId) throws Auth0Exception {

    final MikvahUser user = getUser(auth0UserId);
    return convertToUserDto(user);
  }

  public MikvahUser getUser(final String auth0UserId) throws Auth0Exception {

    Optional<MikvahUser> user = userRepository.getByAuth0UserId(auth0UserId);
    if (!user.isPresent()) {
      final String email = getUserEmail(auth0UserId);
      user = userRepository.findByEmail(email);
      if (!user.isPresent()) {
        final MikvahUser newUser = new MikvahUser();
        newUser.setAuth0UserId(auth0UserId);
        newUser.setEmail(email);
        emailService.sendWelcomeEmail(newUser);
        return userRepository.save(newUser);
      } else {
        final MikvahUser partialUser = user.get();
        partialUser.setAuth0UserId(auth0UserId);
        return userRepository.save(partialUser);
      }

    }
    return user.get();

  }

  public void saveUser(final MikvahUser user) {

    userRepository.save(user);

  }

  private UserDto convertToUserDto(final MikvahUser user) {

    final Optional<CreditCard> card = creditCardService.getCreditCard(user);
    final CreditCard defaultCard = card.orElse(null);

    AppointmentSlotDto currentAppointment = null;
    final LocalDateTime now = LocalDateTime.now(Clock.system(ZoneId.of(config.getTimeZone())));
    final LocalDateTime thirtyDaysFromNow = now.plusDays(30);
    final List<AppointmentSlot> appointments = appointmentSlotRepository
        .findByStartBetweenAndMikvahUserOrderByStartAsc(now, thirtyDaysFromNow, user);
    if (!CollectionUtils.isEmpty(appointments)) {
      final AppointmentSlot appointment = appointments.get(appointments.size() - 1);

      final LocalDate appointmentDay = appointment.getStart().toLocalDate();
      final Optional<DailyHours> hours = dailyHoursService.getHoursForDay(appointmentDay);
      final LocalTime openingTime = hours.get().getOpeningLocalTime().get();
      final ZonedDateTime lastCancellation = LocalDateTime.of(appointmentDay, openingTime)
          .atZone(ZoneId.of(config.getTimeZone()));

      currentAppointment = AppointmentSlotDto.builder()
          .id(appointment.getId())
          .start(appointment.getStart())
          .roomType(appointment.getRoomType())
          .build();
      currentAppointment.setLastCancellation(lastCancellation);
    }

    Plan plan = null;
    LocalDateTime expirationDate = null;
    boolean membershipAutoRenewalEnabled = true;
    final Optional<Membership> membership = membershipRepository.findByMikvahUser(user);
    if (membership.isPresent()) {
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
        .admin(user.isAdmin())
        .stripeCustomerId(user.getStripeCustomerId())
        .title(user.getTitle())
        .defaultCard(defaultCard)
        .currentAppointment(currentAppointment)
        .membershipExpirationDate(expirationDate)
        .membershipPlan(plan)
        .membershipAutoRenewalEnabled(membershipAutoRenewalEnabled)
        .build();
  }

  public MikvahUser createUser(final UserCreationRequestDto request) {

    final String email = request.getEmail();
    final Optional<MikvahUser> existingUser = userRepository.findByEmail(email);
    if (existingUser.isPresent()) {
      throw new IllegalArgumentException("A user with that email already exists.");
    }
    final MikvahUser user = new MikvahUser();
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
