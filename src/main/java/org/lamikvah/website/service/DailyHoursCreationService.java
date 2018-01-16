package org.lamikvah.website.service;

import java.sql.Date;
import java.sql.Time;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;

import org.lamikvah.website.dao.DailyHoursRepository;
import org.lamikvah.website.data.DailyHours;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class DailyHoursCreationService {
    
    private static final long ONE_HOUR = 3600000;
    
    @Autowired
    private DailyHoursRepository repo;

    @Scheduled(initialDelay = 0, fixedRate=ONE_HOUR)
    public void createHours() {
        LocalDate firstDay = LocalDate.now().with(TemporalAdjusters.previous(DayOfWeek.SUNDAY));
        for(int i = 0; i < 14; i++) {
            LocalDate day = firstDay.plusDays(i);
            log.debug("Finding hours for {}", day);
            DailyHours hours = repo.findOne(Date.valueOf(day));
            if(hours == null) {
                log.debug("No hours found for {}. Creating...");
                hours = new DailyHours();
                LocalTime start = LocalTime.of(17, 30);
                LocalTime end = LocalTime.of(21, 30);
                hours.setDay(Date.valueOf(day));
                hours.setOpening(Time.valueOf(start));
                hours.setClosing(Time.valueOf(end));
                hours.setClosed(false);
                DailyHours saved = repo.save(hours);
                log.debug("Created: {}", saved);
            }
        }
    }
}
