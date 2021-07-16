package org.lamikvah.website.data;

import lombok.Builder;
import lombok.Value;

@Builder
@Value
public class AdminAppointmentView {
    String time;

    String roomType;

    String title;

    String firstName;

    String lastName;

    String phoneNumber;

    String email;

    String notes;
}
