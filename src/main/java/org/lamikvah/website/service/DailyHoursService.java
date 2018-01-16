package org.lamikvah.website.service;

import java.sql.Date;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

import org.lamikvah.website.dao.DailyHoursRepository;
import org.lamikvah.website.data.DailyHours;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class DailyHoursService {

    @Autowired private DailyHoursRepository repo;
    
    public List<DailyHours> getHoursForCurrentWeek(){
        
        LocalDate startLocalDate = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY));
        LocalDate endLocalDate = startLocalDate.plusWeeks(1);
        
        Date start = Date.valueOf(startLocalDate);
        Date end = Date.valueOf(endLocalDate);
        
        return repo.findByDayBetweenOrderByDayAsc(start, end);
    }
}
