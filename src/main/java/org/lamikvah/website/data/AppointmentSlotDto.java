package org.lamikvah.website.data;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AppointmentSlotDto {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    long id;

    LocalDateTime start;

    String notes;

    String lastCancellation;

    RoomType roomType;

    public void setLastCancellation(final ZonedDateTime zdt) {

        lastCancellation = FORMATTER.format(zdt);
    }

}
