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
    
    String auth0UserId;
    
    String stripeCustomerId;
    
    boolean member;
    
    CreditCard defaultCard;
    
    AppointmentSlotDto currentAppointment;
    
}
