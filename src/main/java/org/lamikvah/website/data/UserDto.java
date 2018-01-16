package org.lamikvah.website.data;

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

    CreditCard defaultCard;

    AppointmentSlotDto currentAppointment;

}
