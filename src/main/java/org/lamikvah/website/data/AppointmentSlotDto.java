package org.lamikvah.website.data;

import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class AppointmentSlotDto {

    long id;
    
    LocalDateTime start;
    
    String notes;
    
}
