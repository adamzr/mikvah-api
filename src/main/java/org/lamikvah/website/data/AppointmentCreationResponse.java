package org.lamikvah.website.data;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class AppointmentCreationResponse {

    AppointmentSlotDto slot;
    String message;
    
}
