package org.lamikvah.website.dao;

import java.time.LocalDateTime;
import java.util.List;

import org.lamikvah.website.data.AppointmentSlot;
import org.lamikvah.website.data.MikvahUser;
import org.springframework.data.repository.CrudRepository;

public interface AppointmentSlotRepository extends CrudRepository<AppointmentSlot, Long>{
    
    List<AppointmentSlot> findByStartBetweenAndMikvahUserOrderByStartAsc(LocalDateTime start, LocalDateTime end, MikvahUser mikvahUser);
    
    List<AppointmentSlot> findByStartAndMikvahUserIsNullOrderByIdAsc(LocalDateTime start);

    List<AppointmentSlot> findByStartBetweenOrderByStartAsc(LocalDateTime start, LocalDateTime end);

}
