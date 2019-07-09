package org.lamikvah.website.service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.lamikvah.website.MikvahConfiguration;
import org.lamikvah.website.dao.AppointmentSlotRepository;
import org.lamikvah.website.dao.ReservationHistoryLogRepository;
import org.lamikvah.website.data.AppointmentAction;
import org.lamikvah.website.data.AppointmentRequest;
import org.lamikvah.website.data.AppointmentSlot;
import org.lamikvah.website.data.AppointmentSlotDto;
import org.lamikvah.website.data.AttendentAppointmentView;
import org.lamikvah.website.data.DailyHours;
import org.lamikvah.website.data.MikvahUser;
import org.lamikvah.website.data.ReservationHistoryLog;
import org.lamikvah.website.exception.AppointmentCreationException;
import org.lamikvah.website.exception.ServerErrorException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import com.stripe.exception.ApiConnectionException;
import com.stripe.exception.ApiException;
import com.stripe.exception.AuthenticationException;
import com.stripe.exception.CardException;
import com.stripe.exception.InvalidRequestException;
import com.stripe.exception.StripeException;
import com.stripe.model.Charge;
import com.stripe.model.Refund;
import com.stripe.param.ChargeCreateParams;
import com.stripe.param.RefundCreateParams;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class AppointmentService {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("h:mm a");

    @Autowired
    private MikvahConfiguration config;

    @Autowired
    private AppointmentSlotRepository appointmentSlotRepository;

    @Autowired
    private DailyHoursService dailyHoursService;

    @Autowired
    private ReservationHistoryLogRepository reservationHistoryLogRepository;

    @Autowired
    private EmailService emailService;

    public List<LocalDateTime> getAvailableTimes() {

        final LocalDateTime now = LocalDateTime.now(Clock.system(ZoneId.of(config.getTimeZone())));
        final Optional<DailyHours> hoursToday = dailyHoursService.getHoursForDay(now.toLocalDate());
        LocalDateTime start = now;
        if (hoursToday.isPresent()) {
            final DailyHours hours = hoursToday.get();
            if (hours.getOpeningLocalTime().isPresent() && hours.getClosingLocalTime().isPresent()) {
                if (hours.getOpeningLocalTime().get().isBefore(now.toLocalTime())) {
                    start = LocalDateTime.of(now.toLocalDate(), hours.getClosingLocalTime().get().plusMinutes(1));
                }
            }
        }

        final LocalDateTime end = start.plusDays(8);
        final List<AppointmentSlot> slots = appointmentSlotRepository
                .findByStartBetweenAndMikvahUserOrderByStartAsc(start, end, null);
        return slots.stream().map(AppointmentSlot::getStart).distinct().sorted().collect(Collectors.toList());

    }

    @Transactional(isolation = Isolation.SERIALIZABLE, timeout = 10)
    public AppointmentSlotDto createAppointment(final AppointmentRequest appointmentRequest, final MikvahUser user) {

        final LocalDateTime requestedTime = appointmentRequest.getTime();
        final LocalDate requestedDate = requestedTime.toLocalDate();
        final LocalDateTime now = LocalDateTime.now(ZoneId.of(config.getTimeZone()));
        if (now.toLocalDate().isEqual(requestedDate)) {
            // Appointment is for today
            final Optional<DailyHours> dailyHours = dailyHoursService.getHoursForDay(requestedDate);
            if (dailyHours.isPresent()) {
                final Optional<LocalTime> openingTime = dailyHours.get().getOpeningLocalTime();
                if (openingTime.isPresent()) {
                    if (now.toLocalTime().isAfter(openingTime.get())) {
                        log.info("User tried to make an appointment for today, but the mikvah is already open.");
                        throw new AppointmentCreationException(
                                "Appointments can not be made for today once the mikvah is already open.");
                    }
                }
            }
        }

        final List<AppointmentSlot> possibleSlots = appointmentSlotRepository
                .findByStartAndMikvahUserIsNullOrderByIdAsc(requestedTime);
        if (CollectionUtils.isEmpty(possibleSlots)) {
            log.warn("User {} tried to make an appointment {} but there were no appoitment slots available!", user,
                    appointmentRequest);
            throw new AppointmentCreationException(
                    "There were no available appointments for the requested time. Please try a different time.");
        }
        final Optional<String> stripeChargeId = handleUserPayment(user);

        final AppointmentSlot slot = possibleSlots.get(0);
        slot.setMikvahUser(user);
        slot.setNotes(appointmentRequest.getNotes());
        if (stripeChargeId.isPresent()) {
            slot.setStripeChargeId(stripeChargeId.get());
        }
        final AppointmentSlot savedSlot = appointmentSlotRepository.save(slot);

        final ReservationHistoryLog reservationHistoryLog = ReservationHistoryLog.builder()
                .action(AppointmentAction.MADE).appointmentSlot(slot).created(LocalDateTime.now(Clock.systemUTC()))
                .mikvahUser(user).build();

        if (stripeChargeId.isPresent()) {
            reservationHistoryLog.setStripeId(stripeChargeId.get());
        }
        reservationHistoryLogRepository.save(reservationHistoryLog);

        emailService.sendAppointmentConfirmationEmail(user, savedSlot);

        log.info("User {} made appointment {}", user, savedSlot);

        return AppointmentSlotDto.builder().id(savedSlot.getId()).start(savedSlot.getStart())
                .notes(savedSlot.getNotes()).build();
    }

    @Transactional(isolation = Isolation.SERIALIZABLE, timeout = 10)
    public Optional<String> cancelAppointment(final MikvahUser user, final long slotId) {

        final Optional<AppointmentSlot> existingSlot = appointmentSlotRepository.findById(slotId);
        if (!existingSlot.isPresent())
            return Optional.empty();
        final AppointmentSlot slot = existingSlot.get();
        if (slot.getMikvahUser() == null)
            return Optional.empty();
        if (!slot.getMikvahUser().equals(user))
            throw new ServerErrorException("You can only cancel your own appointment.");
        final LocalDateTime now = LocalDateTime.now(ZoneId.of(config.getTimeZone()));
        final LocalDate slotDate = slot.getStart().toLocalDate();
        if (now.toLocalDate().isEqual(slotDate)) {
            // Appointment is for today
            final Optional<DailyHours> dailyHours = dailyHoursService.getHoursForDay(slotDate);
            if (dailyHours.isPresent()) {
                final Optional<LocalTime> openingTime = dailyHours.get().getOpeningLocalTime();
                if (openingTime.isPresent()) {
                    if (now.toLocalTime().isAfter(openingTime.get())) {
                        log.info("User tried to cancel an appointment for today, but the mikvah is already open.");
                        throw new ServerErrorException(
                                "Appointments can not be cancelled on the day they are for once the mikvah is open.");
                    }
                }
            }
        }
        Optional<String> refundId = Optional.empty();
        if (!StringUtils.isEmpty(slot.getStripeChargeId())) {
            refundId = refundCharge(slot.getStripeChargeId());
            log.info("User {} was refunded for cancelled. Refund ID: ", user, refundId);
        }
        slot.setMikvahUser(null);
        slot.setStripeChargeId(null);
        slot.setNotes(null);
        appointmentSlotRepository.save(slot);

        final ReservationHistoryLog reservationHistoryLog = ReservationHistoryLog.builder()
                .action(AppointmentAction.CANCELED).appointmentSlot(slot).created(LocalDateTime.now(Clock.systemUTC()))
                .mikvahUser(user).build();

        if (refundId.isPresent()) {
            reservationHistoryLog.setStripeId(refundId.get());
        }
        reservationHistoryLogRepository.save(reservationHistoryLog);

        emailService.sendAppointmentCancellationEmail(user, slot, refundId);

        log.info("User {} cancelled appointment {}", user, slot);

        return refundId;
    }

    private Optional<String> refundCharge(final String stripeChargeId) {

        final RefundCreateParams params = RefundCreateParams.builder().setCharge(stripeChargeId).build();
        Refund refund;
        try {
            refund = Refund.create(params);
        } catch (final StripeException e) {
            log.error("We failed to refund charge={}!!!!", stripeChargeId, e);
            return Optional.empty();
        }
        return Optional.of(refund.getId());
    }

    private Optional<String> handleUserPayment(final MikvahUser user) {

        if (user.isMember())
            return Optional.empty();

        // Charge the user's card:
        final ChargeCreateParams params = ChargeCreateParams.builder()
                .setAmount((long) config.getAppointmentCost())
                .setCurrency(config.getCurrency())
                .setCustomer(user.getStripeCustomerId())
                .setStatementDescriptor("Appointment").build();
        try {
            final Charge charge = Charge.create(params);
            return Optional.of(charge.getId());
        } catch (AuthenticationException | InvalidRequestException | ApiConnectionException | ApiException e) {
            log.error("Payment processing error.", e);
            throw new AppointmentCreationException(
                    "There was a problem processing your payment. Please try again later.");
        } catch (final CardException e) {
            log.info("Card processing error.", e);
            throw new AppointmentCreationException(
                    "There was a problem processing your payment. " + e.getLocalizedMessage());
        } catch (final StripeException e) {
            log.error("Payment processing error.", e);
            throw new AppointmentCreationException(
                    "There was a problem processing your payment. Please try again later.");
        }

    }

    public List<AttendentAppointmentView> getAppointmentsForAttendants(final LocalDateTime now) {

        final List<AppointmentSlot> todaysSlots = appointmentSlotRepository.findByStartBetweenOrderByStartAsc(
                now.toLocalDate().atStartOfDay(), now.toLocalDate().atTime(LocalTime.MAX));
        return todaysSlots.stream().filter(slot -> slot.getMikvahUser() != null)
                .map(slot -> AttendentAppointmentView.builder()
                        .firstName(slot.getMikvahUser().getFirstName())
                        .time(slot.getStart().toLocalTime().format(TIME_FORMAT))
                        .notes(slot.getNotes()).build())
                .sorted(Comparator.comparing(AttendentAppointmentView::getTime))
                .collect(Collectors.toList());

    }

}
