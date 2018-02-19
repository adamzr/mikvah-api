package org.lamikvah.website.data;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class AttendentAppointmentView {

    String time;

    String firstName;

    String notes;

}
