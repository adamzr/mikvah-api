package org.lamikvah.website.data;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class AppointmentRequest {

    private LocalDateTime time;
    
}
