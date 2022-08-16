package org.lamikvah.website.data;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UpdateAppointmentRequest {

    private LocalDateTime time;

    private String notes;

}
