package org.lamikvah.website.data;

import java.time.LocalDateTime;
import java.util.Comparator;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AvailableDateTimeAndRoomType implements Comparable<AvailableDateTimeAndRoomType> {

    private static final Comparator<AvailableDateTimeAndRoomType> NATURAL_ORDER_COMPARATOR = Comparator
            .comparing(AvailableDateTimeAndRoomType::getDateTime)
            .thenComparing(AvailableDateTimeAndRoomType::getRoomType);

    private final LocalDateTime dateTime;

    private final RoomType roomType;

    @Override
    public int compareTo(final AvailableDateTimeAndRoomType other) {

        return NATURAL_ORDER_COMPARATOR.compare(this, other);
    }

}
