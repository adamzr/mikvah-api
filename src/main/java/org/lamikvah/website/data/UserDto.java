package org.lamikvah.website.data;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class UserDto {

    Long id;

    String title;

    String firstName;

    String lastName;

    String email;

    String notes;

    String phoneNumber;

    String addressLine1;

    String addressLine2;

    String city;

    String stateCode;

    String postalCode;

    String countryCode;

    String auth0UserId;

    String stripeCustomerId;

    boolean member;

    Plan membershipPlan;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    LocalDateTime membershipExpirationDate;

    boolean membershipAutoRenewalEnabled;

    CreditCard defaultCard;

    AppointmentSlotDto currentAppointment;

}
