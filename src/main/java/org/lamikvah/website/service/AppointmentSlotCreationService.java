package org.lamikvah.website.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import org.lamikvah.website.dao.AppointmentSlotRepository;
import org.lamikvah.website.data.AppointmentSlot;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

@Component
public class AppointmentSlotCreationService {
    
    private static final long ONE_HOUR = 3600000;
    
    @Autowired
    private AppointmentSlotRepository repo;

    @Scheduled(initialDelay = 0, fixedRate=ONE_HOUR)
    public void createSlots() {
        LocalDate day = LocalDate.now();
        for(int i = 0; i < 9; i++) {
            day = day.plusDays(i);
            LocalDateTime start = day.atStartOfDay();
            LocalDateTime end = day.plusDays(1).atStartOfDay();
            List<AppointmentSlot> slots = repo.findByStartBetweenOrderByStartAsc(start, end);
            if(CollectionUtils.isEmpty(slots)) {
                for(int j = 0; j < 4; j++) {
                    AppointmentSlot newSlot = new AppointmentSlot();
                    LocalDateTime startTime = day.atTime(LocalTime.of(19 + j, 0));
                    newSlot.setStart(startTime);
                    repo.save(newSlot);
                }
            }
        }
    }
}
