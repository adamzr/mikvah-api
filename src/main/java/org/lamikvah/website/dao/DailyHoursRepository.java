package org.lamikvah.website.dao;

import java.sql.Date;
import java.util.List;

import org.lamikvah.website.data.DailyHours;
import org.springframework.data.repository.CrudRepository;

public interface DailyHoursRepository extends CrudRepository<DailyHours, Date> {

    public List<DailyHours> findByDayBetweenOrderByDayAsc(Date start, Date end);
    
}
