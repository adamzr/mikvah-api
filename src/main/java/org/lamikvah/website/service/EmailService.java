package org.lamikvah.website.service;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import com.stripe.model.Invoice;
import java.io.StringWriter;
import java.text.NumberFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.lamikvah.website.MikvahConfiguration;
import org.lamikvah.website.dao.MikvahUserRepository;
import org.lamikvah.website.data.AppointmentSlot;
import org.lamikvah.website.data.CreditCard;
import org.lamikvah.website.data.Membership;
import org.lamikvah.website.data.MikvahUser;
import org.simplejavamail.email.Email;
import org.simplejavamail.email.EmailBuilder;
import org.simplejavamail.email.Recipient;
import org.simplejavamail.mailer.Mailer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Slf4j
@Component
public class EmailService {

  private static final String EXPIRATION_DATE = "expirationDate";

  private static final String AMOUNT = "amount";

  private static final String LAST4 = "last4";

  private static final int CENTS_PER_DOLLAR = 100;

  private static final DateTimeFormatter FRIENDLY_FORMAT = DateTimeFormatter
      .ofPattern("EEEE, MMMM, d, yyyy 'at' h:mm a");

  @Autowired
  private Mailer mailer;

  @Autowired
  private MikvahConfiguration config;

  @Autowired
  @Lazy
  private CreditCardService creditCardService;

  @Autowired
  private MikvahUserRepository userRepository;

  private final MustacheFactory mf = new DefaultMustacheFactory();
  private final NumberFormat currencyFormatter = NumberFormat.getCurrencyInstance();

  private final LoadingCache<String, Mustache> mustacheTemplateCache =
      Caffeine.newBuilder().maximumSize(100)
          .build(templateFileName -> mf.compile(templateFileName));

  @Async
  public void sendAppointmentConfirmationEmail(final MikvahUser user,
      final AppointmentSlot appointment) {

    try {

      final Mustache htmlMustache =
          mustacheTemplateCache.get("emails/appointment-confirmation.html.mustache");

      final Mustache txtMustache =
          mustacheTemplateCache.get("emails/appointment-confirmation.txt.mustache");

      final Map<String, Object> context = new HashMap<>();
      context.put("appointmentSlotId", appointment.getId());
      context.put("fullName", user.getFullName());
      context.put("isoStartTime", appointment.getStart().atZone(ZoneId.of(config.getTimeZone()))
          .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
      context.put("isoModifiedTime", LocalDateTime.now().atZone(ZoneId.of(config.getTimeZone()))
          .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
      context.put("startDateTime",
          appointment.getStart().atZone(ZoneId.of(config.getTimeZone())).format(FRIENDLY_FORMAT));
      context.put("ccCharged", !StringUtils.isEmpty(appointment.getStripeChargeId()));
      final Optional<CreditCard> creditCardOptional = creditCardService.getCreditCard(user);
      if (creditCardOptional.isPresent()) {
        final CreditCard creditCard = creditCardOptional.get();
        context.put("cardType", creditCard.getBrand());
        context.put(LAST4, creditCard.getLast4());
      }
      context.put("confirmationCode", appointment.getStripeChargeId());
      context.put(AMOUNT, currencyFormatter.format(config.getAppointmentCost() / CENTS_PER_DOLLAR));

      sendEmail(user, htmlMustache, txtMustache, context, "Your Appointment Is Confirmed!");

    } catch (final Exception e) {
      log.error("There was a problem sending the appointment confirmation email.", e);
    }
  }

  @Async
  public void sendAppointmentCancellationEmail(final MikvahUser user,
      final AppointmentSlot appointment,
      final Optional<String> refundId) {

    try {

      final Mustache htmlMustache =
          mustacheTemplateCache.get("emails/appointment-cancellation.html.mustache");

      final Mustache txtMustache =
          mustacheTemplateCache.get("emails/appointment-cancellation.txt.mustache");

      final Map<String, Object> context = new HashMap<>();
      context.put("appointmentSlotId", appointment.getId());
      context.put("fullName", user.getFullName());
      context.put("isoStartTime", appointment.getStart().atZone(ZoneId.of(config.getTimeZone()))
          .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
      context.put("isoModifiedTime", LocalDateTime.now().atZone(ZoneId.of(config.getTimeZone()))
          .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
      context.put("startDateTime",
          appointment.getStart().atZone(ZoneId.of(config.getTimeZone())).format(FRIENDLY_FORMAT));
      context.put("ccRefunded", refundId.isPresent());
      context.put("confirmationCode", refundId.orElse(null));
      context.put(AMOUNT, currencyFormatter.format(config.getAppointmentCost() / CENTS_PER_DOLLAR));

      sendEmail(user, htmlMustache, txtMustache, context, "You're Appointment Has Been Cancelled");

    } catch (final Exception e) {
      log.error("There was a problem sending the appointment cancellation email.", e);
    }
  }

  @Async
  public void sendWelcomeEmail(final MikvahUser user) {

    try {

      final Mustache htmlMustache = mustacheTemplateCache.get("emails/welcome.html.mustache");

      final Mustache txtMustache = mustacheTemplateCache.get("emails/welcome.txt.mustache");

      final Map<String, Object> context = new HashMap<>();

      sendEmail(user, htmlMustache, txtMustache, context,
          "Welcome to the Los Angeles Mikvah Society - Mikvat Esteher Website");

    } catch (final Exception e) {
      log.error("There was a problem sending the welcome email.", e);
    }
  }

  @Async
  public void sendNewMemberEmail(final MikvahUser user, final Membership membership) {
    try {

      final Mustache htmlMustache =
          mustacheTemplateCache.get("emails/new-membership.html.mustache");

      final Mustache txtMustache = mustacheTemplateCache.get("emails/new-membership.txt.mustache");

      final Map<String, Object> context = new HashMap<>();
      context.put("level", membership.getPlan().name());
      context.put(EXPIRATION_DATE, membership.getExpiration().format(FRIENDLY_FORMAT));
      final Optional<CreditCard> creditCard = creditCardService.getCreditCard(user);
      if (creditCard.isPresent()) {
        context.put(LAST4, creditCard.get().getLast4());
      } else {
        context.put(LAST4, "XXXX");
      }
      context.put("autoRenewEnabled", membership.isAutoRenewEnabled());
      context.put("date", FRIENDLY_FORMAT.format(LocalDateTime.now()));
      context.put(AMOUNT, membership.getPlan().getFormattedPrice());
      sendEmail(user, htmlMustache, txtMustache, context, "Mikvah Membership");

    } catch (final Exception e) {

      log.error("There was a problem sending the mikvah membership start email.", e);

    }

    sendMembershipPaymentNotificationEmail(user.getFullName(),
        membership.getPlan().getFormattedPrice());

  }

  @Async
  public void sendMembershipRenewalEmail(final MikvahUser user, final Membership membership) {
    try {

      final Mustache htmlMustache =
          mustacheTemplateCache.get("emails/membership-renewal.html.mustache");

      final Mustache txtMustache =
          mustacheTemplateCache.get("emails/membership-renewal.txt.mustache");

      final Map<String, Object> context = new HashMap<>();
      context.put("level", membership.getPlan().name());
      context.put(EXPIRATION_DATE, membership.getExpiration().format(FRIENDLY_FORMAT));
      final Optional<CreditCard> creditCard = creditCardService.getCreditCard(user);
      if (creditCard.isPresent()) {
        context.put(LAST4, creditCard.get().getLast4());
      } else {
        context.put(LAST4, "XXXX");
      }
      context.put("autoRenewEnabled", membership.isAutoRenewEnabled());
      context.put("date", FRIENDLY_FORMAT.format(LocalDateTime.now()));
      context.put(AMOUNT, membership.getPlan().getFormattedPrice());

      sendEmail(user, htmlMustache, txtMustache, context, "Mikvah Membership Renewal");

    } catch (final Exception e) {

      log.error("There was a problem sending the mikvah membership renewal email.", e);

    }

    sendMembershipPaymentNotificationEmail(user.getFullName(),
        membership.getPlan().getFormattedPrice());

  }

  @Async
  public void sendCreditCardUpdateEmail(final MikvahUser user,
      final Optional<Membership> membership) {
    try {

      final Mustache htmlMustache =
          mustacheTemplateCache.get("emails/credit-card-update.html.mustache");

      final Mustache txtMustache =
          mustacheTemplateCache.get("emails/credit-card-update.txt.mustache");

      final Map<String, Object> context = new HashMap<>();

      context.put("isAutoRenewingMember",
          membership.isPresent() && membership.get().isAutoRenewEnabled());

      sendEmail(user, htmlMustache, txtMustache, context, "Credit Card Update");

    } catch (final Exception e) {

      log.error(
          "There was a problem sending the mikvah membership credit card update notification email.",
          e);

    }
  }

  @Async
  public void sendAutoRenewDisabledEmail(final MikvahUser user, final Membership membership) {
    try {

      final Mustache htmlMustache =
          mustacheTemplateCache.get("emails/auto-renew-disabled.html.mustache");

      final Mustache txtMustache =
          mustacheTemplateCache.get("emails/auto-renew-disabled.txt.mustache");

      final Map<String, Object> context = new HashMap<>();
      context.put(EXPIRATION_DATE, FRIENDLY_FORMAT.format(membership.getExpiration()));

      sendEmail(user, htmlMustache, txtMustache, context, "Membership Automatic Renewal Disabled");

    } catch (final Exception e) {

      log.error("There was a problem sending the mikvah membership auto-renew disabled email.", e);

    }
  }

  @Async
  public void sendAutoRenewEnabledEmail(final MikvahUser user, final Membership membership) {
    try {

      final Mustache htmlMustache =
          mustacheTemplateCache.get("emails/auto-renew-enabled.html.mustache");

      final Mustache txtMustache =
          mustacheTemplateCache.get("emails/auto-renew-enabled.txt.mustache");

      final Map<String, Object> context = new HashMap<>();
      context.put(EXPIRATION_DATE, FRIENDLY_FORMAT.format(membership.getExpiration()));

      sendEmail(user, htmlMustache, txtMustache, context, "Membership Automatic Renewal Enabled");

    } catch (final Exception e) {

      log.error("There was a problem sending the mikavh membership auto-reneal enabled email.", e);

    }

  }

  @Async
  public void sendUpcomingRenewalEmail(final Invoice upcomingInvoice) {
    try {

      final String customerId = upcomingInvoice.getCustomer();
      final Optional<MikvahUser> userOptional = userRepository.findByStripeCustomerId(customerId);
      if (!userOptional.isPresent()) {
        log.error("Can't send email to unidentified user with upcoming renewal.");
        return;
      }
      final MikvahUser user = userOptional.get();

      final Mustache htmlMustache =
          mustacheTemplateCache.get("emails/upcoming-renewal.html.mustache");

      final Mustache txtMustache =
          mustacheTemplateCache.get("emails/upcoming-renewal.txt.mustache");

      final Map<String, Object> context = new HashMap<>();

      final Instant i = Instant.ofEpochSecond(upcomingInvoice.getNextPaymentAttempt());
      final ZonedDateTime renewalDate = ZonedDateTime.ofInstant(i, ZoneId.of(config.getTimeZone()));
      final String formattedRenewalDate = FRIENDLY_FORMAT.format(renewalDate);

      context.put("renewalDate", formattedRenewalDate);
      context.put(AMOUNT,
          currencyFormatter.format(upcomingInvoice.getAmountDue() / CENTS_PER_DOLLAR));

      sendEmail(user, htmlMustache, txtMustache, context, "Upcoming Mikvah Membership Renewal");

    } catch (final Exception e) {

      log.error(
          "There was a problem sending the mikvah membership upcoming renewal notification email.",
          e);

    }
  }

  @Async
  public void sendMembershipEndedEmail(final MikvahUser user) {

    try {
      final Mustache htmlMustache =
          mustacheTemplateCache.get("emails/membership-ended.html.mustache");

      final Mustache txtMustache =
          mustacheTemplateCache.get("emails/membership-ended.txt.mustache");

      final Map<String, Object> context = new HashMap<>();

      sendEmail(user, htmlMustache, txtMustache, context, "Membership Ended");

    } catch (final Exception e) {
      log.error("There was a problem sending the membership ended email.", e);
    }

  }

  @Async
  public void sendDonationEmail(final String name, final String email, final double amount) {

    final String formattedAmount = NumberFormat.getCurrencyInstance().format(amount);
    // Receipt to donor
    try {
      final Mustache htmlMustache =
          mustacheTemplateCache.get("emails/donation-receipt.html.mustache");

      final Mustache txtMustache =
          mustacheTemplateCache.get("emails/donation-receipt.txt.mustache");

      final Map<String, Object> context = new HashMap<>();
      context.put("name", name);
      context.put(AMOUNT, formattedAmount);
      context.put("date", FRIENDLY_FORMAT.format(LocalDateTime.now()));

      sendEmail(name, email, htmlMustache, txtMustache, context, "Donation Receipt");

    } catch (final Exception e) {
      log.error("There was a problem sending the donation receipt email.", e);
    }

    // Notification to mikvah staff
    try {
      final Mustache htmlMustache =
          mustacheTemplateCache.get("emails/donation-notification.html.mustache");

      final Mustache txtMustache =
          mustacheTemplateCache.get("emails/donation-notification.txt.mustache");

      final Map<String, Object> context = new HashMap<>();
      context.put("name", name);
      context.put("donorEmail", email);
      context.put(AMOUNT, formattedAmount);
      context.put("date", FRIENDLY_FORMAT.format(LocalDateTime.now()));
      sendEmail("Mikvah Treasurer", config.getMikvahTreasurerEmail(), htmlMustache, txtMustache,
          context,
          "Donation Notification");

    } catch (final Exception e) {

      log.error("There was a problem sending the donation notification email.", e);

    }

  }

  private void sendMembershipPaymentNotificationEmail(final String name,
      final String formattedAmount) {
    // Notification to mikvah staff
    try {
      final Mustache htmlMustache =
          mustacheTemplateCache.get("emails/membership-notification.html.mustache");

      final Mustache txtMustache =
          mustacheTemplateCache.get("emails/membership-notification.txt.mustache");

      final Map<String, Object> context = new HashMap<>();
      context.put("name", name);
      context.put(AMOUNT, formattedAmount);
      context.put("date", FRIENDLY_FORMAT.format(LocalDateTime.now()));
      final Recipient treasurer = new Recipient("Mikvah Treasurer",
          config.getMikvahTreasurerEmail(), javax.mail.Message.RecipientType.TO);
      final Recipient membershipManager = new Recipient("Membership Manager",
          config.getMembershipManagerEmail(), javax.mail.Message.RecipientType.TO);
      final Collection<Recipient> recipients = Arrays.asList(treasurer, membershipManager);
      sendEmail(recipients, htmlMustache, txtMustache, context,
          "Donation Notification");

    } catch (final Exception e) {

      log.error("There was a problem sending the donation notification email.", e);

    }
  }

  private void sendEmail(final MikvahUser user, final Mustache htmlMustache,
      final Mustache txtMustache, final Map<String, Object> context,
      final String subject) {

    sendEmail(user.getFullName(), user.getEmail(), htmlMustache, txtMustache, context, subject);

  }

  private void sendEmail(final String name, final String emailAddress, final Mustache htmlMustache,
      final Mustache txtMustache,
      final Map<String, Object> context, final String subject) {

    final StringWriter htmlWriter = new StringWriter();
    final StringWriter txtWriter = new StringWriter();

    htmlMustache.execute(htmlWriter, context);
    txtMustache.execute(txtWriter, context);

    final Email email = EmailBuilder.startingBlank()
        .from("Los Angeles Mikvah Society", config.getFromEmailAddress())
        .to(name, emailAddress)
        .withSubject(subject)
        .withPlainText(txtWriter.toString())
        .withHTMLText(htmlWriter.toString()).buildEmail();

    mailer.sendMail(email, true);
  }

  private void sendEmail(final Collection<Recipient> recipients, final Mustache htmlMustache,
      final Mustache txtMustache,
      final Map<String, Object> context, final String subject) {

    final StringWriter htmlWriter = new StringWriter();
    final StringWriter txtWriter = new StringWriter();

    htmlMustache.execute(htmlWriter, context);
    txtMustache.execute(txtWriter, context);

    final Email email = EmailBuilder.startingBlank()
        .from("Los Angeles Mikvah Society", config.getFromEmailAddress())
        .to(recipients)
        .withSubject(subject)
        .withPlainText(txtWriter.toString())
        .withHTMLText(htmlWriter.toString()).buildEmail();

    mailer.sendMail(email, true);
  }

}
