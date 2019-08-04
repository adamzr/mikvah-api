package org.lamikvah.website.dao;

import java.time.LocalDateTime;
import java.util.List;

import org.lamikvah.website.data.AppointmentSlot;
import org.lamikvah.website.data.MikvahUser;
import org.lamikvah.website.data.RoomType;
import org.springframework.data.repository.CrudRepository;

public interface AppointmentSlotRepository extends CrudRepository<AppointmentSlot, Long> {

    List<AppointmentSlot> findByStartBetweenAndMikvahUserAndRoomTypeOrderByStartAsc(LocalDateTime start,
            LocalDateTime end, MikvahUser mikvahUser, RoomType roomType);

    List<AppointmentSlot> findByStartAndRoomTypeAndMikvahUserIsNullOrderByIdAsc(LocalDateTime start, RoomType roomType);

    List<AppointmentSlot> findByStartBetweenOrderByStartAsc(LocalDateTime start, LocalDateTime end);

    List<AppointmentSlot> findByStartBetweenAndMikvahUserOrderByStartAsc(LocalDateTime start,
            LocalDateTime end, MikvahUser user);

    List<AppointmentSlot> findByStartBetweenAndRoomTypeOrderByStartAsc(LocalDateTime start, LocalDateTime end,
            RoomType roomType);

}
