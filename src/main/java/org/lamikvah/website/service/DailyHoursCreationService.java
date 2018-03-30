package org.lamikvah.website.service;

import java.sql.Date;
import java.sql.Time;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.Set;
import java.util.TimeZone;

import org.lamikvah.website.dao.DailyHoursRepository;
import org.lamikvah.website.data.DailyHours;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.google.common.collect.Sets;

import lombok.extern.slf4j.Slf4j;
import net.sourceforge.zmanim.ComplexZmanimCalendar;
import net.sourceforge.zmanim.hebrewcalendar.JewishCalendar;
import net.sourceforge.zmanim.util.GeoLocation;

@Component
@Slf4j
public class DailyHoursCreationService {

    private static final long ONE_HOUR = 3600000;

    private static final Set<Integer> YOM_TOV_INDEXES = Sets.newHashSet(JewishCalendar.PESACH,
            JewishCalendar.SUCCOS,
            JewishCalendar.SHAVUOS,
            JewishCalendar.ROSH_HASHANA,
            JewishCalendar.SHEMINI_ATZERES,
            JewishCalendar.SIMCHAS_TORAH);


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

    public void createHoursForNext3Weeks() {
        LocalDate start = LocalDate.now().with(TemporalAdjusters.previous(DayOfWeek.SUNDAY));
        calculateHoursForWeek(start);
        for(int i = 0; i < 2; i++) {
            start = start.with(TemporalAdjusters.next(DayOfWeek.SUNDAY));
            calculateHoursForWeek(start);
        }

    }

    private void calculateHoursForWeek(LocalDate day) {

        ComplexZmanimCalendar zmanimCalendar = createCalendar();

        LocalTime latestTzais = getLatestTzaisForWeek(day, zmanimCalendar);

        day = LocalDate.now().with(TemporalAdjusters.previous(DayOfWeek.SUNDAY));
        for(int i = 0; i < 7; i++) {
            day = day.plusDays(i);
            DailyHours hours = repo.findOne(Date.valueOf(day));
            if(hours == null) {
                continue;
            }
            hours = new DailyHours();
            hours.setDay(Date.valueOf(day));
            zmanimCalendar.getCalendar().set(day.getYear(), day.getMonthValue(), day.getDayOfMonth());
            JewishCalendar jewishCalendar = new JewishCalendar(Date.from(day.atStartOfDay(ZoneId.of("America/Los_Angeles")).toInstant()));
            if(isErevYomKippurOrTishaBav(day)) {
                hours.setClosed(true);
                repo.save(hours);
            } else if(isLeilYomTovOrShabbos(day)) {
                LocalTime candleLighting = zmanimCalendar.getCandleLighting().toInstant().atZone(ZoneId.of("America/Los_Angeles")).toLocalTime();
                LocalTime opening = candleLighting.plusHours(1);
                hours.setOpening(Time.valueOf(opening));
                hours.setClosing(Time.valueOf(opening.plusMinutes(30)));
            } else if(isMotzeiYomTovOrShabbos(day)) {
                LocalTime openingTime = latestTzais.plusMinutes(45);
            }
        }

    }

    private LocalTime getLatestTzaisForWeek(LocalDate day, ComplexZmanimCalendar zmanimCalendar) {
        LocalTime latestTzais = LocalTime.MIN;
        for(int i = 0; i < 7; i++) {
            day = day.plusDays(i);
            zmanimCalendar.getCalendar().set(day.getYear(), day.getMonthValue(), day.getDayOfMonth());
            LocalTime tzais = zmanimCalendar.getTzais19Point8Degrees()
                    .toInstant().atZone(ZoneId.of("America/Los_Angeles")).toLocalTime();
            if(tzais.isAfter(latestTzais)) {
                latestTzais = tzais;
            }
        }

        latestTzais.plusMinutes(3);
        int minutesMod5 = latestTzais.getMinute() % 5;
        if(minutesMod5 != 0) {
            latestTzais.plusMinutes(5 - minutesMod5);
        }
        return latestTzais;
    }

    private boolean isErevYomKippurOrTishaBav(LocalDate date) {
        LocalDate nextDay = date.plusDays(1);
        JewishCalendar jewishCalendar = new JewishCalendar(Date.from(nextDay.atStartOfDay(ZoneId.of("America/Los_Angeles")).toInstant()));
        int yomTovIndex = jewishCalendar.getYomTovIndex();
        return yomTovIndex == JewishCalendar.YOM_KIPPUR || yomTovIndex == JewishCalendar.TISHA_BEAV;
    }

    private boolean isLeilYomTovOrShabbos(LocalDate date) {
        if(date.getDayOfWeek() == DayOfWeek.FRIDAY) {
            return true;
        }
        LocalDate nextDay = date.plusDays(1);
        JewishCalendar jewishCalendar = new JewishCalendar(Date.from(nextDay.atStartOfDay(ZoneId.of("America/Los_Angeles")).toInstant()));
        int yomTovIndex = jewishCalendar.getYomTovIndex();
        return YOM_TOV_INDEXES.contains(yomTovIndex);
    }

    private boolean isMotzeiYomTovOrShabbos(LocalDate date) {
        if(date.getDayOfWeek() == DayOfWeek.SATURDAY) {
            return true;
        }
        LocalDate previousDay = date.minusDays(1);
        JewishCalendar jewishCalendar = new JewishCalendar(Date.from(previousDay.atStartOfDay(ZoneId.of("America/Los_Angeles")).toInstant()));
        int yomTovIndex = jewishCalendar.getYomTovIndex();
        return YOM_TOV_INDEXES.contains(yomTovIndex);
    }

    private ComplexZmanimCalendar createCalendar() {
        String locationName = "Los Angeles, CA";
        double latitude = 34.0549987; // Los Angeles, CA
        double longitude = -118.3969812; // Los Angeles, CA
        double elevation = 65; // optional elevation
        TimeZone timeZone = TimeZone.getTimeZone("America/Los_Angeles");
        GeoLocation location = new GeoLocation(locationName, latitude, longitude, elevation, timeZone);
        return new ComplexZmanimCalendar(location);
    }
}
