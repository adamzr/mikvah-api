package org.lamikvah.website.service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.lamikvah.website.dao.AppointmentSlotRepository;
import org.lamikvah.website.data.AppointmentRequest;
import org.lamikvah.website.data.AppointmentSlot;
import org.lamikvah.website.data.AppointmentSlotDto;
import org.lamikvah.website.data.MikvahUser;
import org.lamikvah.website.exception.AppointmentCreationException;
import org.lamikvah.website.exception.ServerErrorException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import com.stripe.exception.APIConnectionException;
import com.stripe.exception.APIException;
import com.stripe.exception.AuthenticationException;
import com.stripe.exception.CardException;
import com.stripe.exception.InvalidRequestException;
import com.stripe.model.Charge;
import com.stripe.model.Refund;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class AppointmentService {

    @Autowired
    private AppointmentSlotRepository appointmentSlotRepository;

    public List<LocalDateTime> getAvailableTimes() {
        LocalDateTime start = LocalDateTime.now();
        LocalDateTime end = start.plusDays(8);
        List<AppointmentSlot> slots = appointmentSlotRepository.findByStartBetweenAndMikvahUserOrderByStartAsc(start,
                end, null);
        return slots.stream().map(AppointmentSlot::getStart).distinct().sorted().collect(Collectors.toList());
    }

    @Transactional(isolation = Isolation.SERIALIZABLE, timeout = 10)
    public AppointmentSlotDto createAppointment(AppointmentRequest appointmentRequest, MikvahUser user) {
        Optional<String> stripeChargeId = handleUserPayment(user);

        LocalDateTime requestedTime = appointmentRequest.getTime();
        List<AppointmentSlot> possibleSlots = appointmentSlotRepository
                .findByStartAndMikvahUserIsNullOrderByIdAsc(requestedTime);
        if (CollectionUtils.isEmpty(possibleSlots)) {
            throw new AppointmentCreationException("There were no available appointments for the requested time. Please try a different time.");
        }
        AppointmentSlot slot = possibleSlots.get(0);
        slot.setMikvahUser(user);
        if(stripeChargeId.isPresent()) {
            slot.setStripeChargeId(stripeChargeId.get());
        }
        AppointmentSlot savedSlot = appointmentSlotRepository.save(slot);
        return AppointmentSlotDto.builder()
                .id(savedSlot.getId())
                .start(savedSlot.getStart())
                .build();
    }
    
    @Transactional(isolation = Isolation.SERIALIZABLE, timeout = 10)
    public void cancelAppointment(MikvahUser user, long slotId) {
        AppointmentSlot slot = appointmentSlotRepository.findOne(slotId);
        if(slot == null) {
            return;
        }
        if(slot.getMikvahUser() == null) {
            return;
        }
        if(!slot.getMikvahUser().equals(user)) {
            throw new ServerErrorException("You can only cancel your own appointment.");
        }
        if(!StringUtils.isEmpty(slot.getStripeChargeId())) {
            refundCharge(slot.getStripeChargeId());
        }
        slot.setMikvahUser(null);
        slot.setStripeChargeId(null);
        appointmentSlotRepository.save(slot);
    }

    private void refundCharge(String stripeChargeId) {

        Map<String, Object> params = new HashMap<>();
        params.put("charge", stripeChargeId);
        try {
            Refund.create(params);
        } catch (AuthenticationException | InvalidRequestException | APIConnectionException | CardException
                | APIException e) {
            log.error("We failed to refund charge={}!!!!", stripeChargeId, e);
        }
        
    }

    private Optional<String> handleUserPayment(MikvahUser user) {
        
        if(user.isMember()) {
            return Optional.empty();
        }

        // Charge the user's card:
        Map<String, Object> chargeParams = new HashMap<>();
        chargeParams.put("amount", 3000);
        chargeParams.put("currency", "usd");
        chargeParams.put("customer", user.getStripeCustomerId());
        try {
            Charge charge = Charge.create(chargeParams);
            return Optional.of(charge.getId());
        } catch (AuthenticationException | InvalidRequestException | APIConnectionException | APIException e) {
            log.error("Payment processing error.", e);
            throw new AppointmentCreationException("There was a problem processing your payment. Please try again later.");
        }  catch (CardException e) {
            log.info("Card processing error.", e);
            throw new AppointmentCreationException("There was a problem processing your payment. " + e.getLocalizedMessage());
        }

    }
}
