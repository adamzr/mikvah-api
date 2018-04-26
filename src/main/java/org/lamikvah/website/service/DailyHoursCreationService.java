package org.lamikvah.website.service;

import java.sql.Date;
import java.sql.Time;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TimeZone;

import org.lamikvah.website.MikvahConfiguration;
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

    // This is the local time zone, it is cached here after being set by getTimezone
    private ZoneId timezone = null;

    private static final long ONE_HOUR = 3600000; // in milliseconds

    static final Set<Integer> YOM_TOV_INDEXES = Sets.newHashSet(JewishCalendar.PESACH,
            JewishCalendar.SUCCOS,
            JewishCalendar.SHAVUOS,
            JewishCalendar.ROSH_HASHANA,
            JewishCalendar.SHEMINI_ATZERES,
            JewishCalendar.SIMCHAS_TORAH);

    /**
     * Mikvah should generally not be open past 11:00 PM
     */
    private static final LocalTime LATEST_CLOSING_TIME = LocalTime.of(23, 0);

    @Autowired
    private DailyHoursRepository repo;

    @Autowired
    private MikvahConfiguration config;

    @Scheduled(initialDelay = 0, fixedRate=ONE_HOUR)
    public void createHoursForNext3Weeks() {

        log.debug("Calculating hours for next 3 weeks.");

        LocalDate start = LocalDate.now().with(TemporalAdjusters.previous(DayOfWeek.SUNDAY));
        calculateHoursForWeek(start);
        for(int i = 0; i < 2; i++) {
            start = start.with(TemporalAdjusters.next(DayOfWeek.SUNDAY));
            calculateHoursForWeek(start);
        }

    }

    private void calculateHoursForWeek(LocalDate sunday) {

        log.debug("Calculating hours for the week starting on {}", sunday);

        List<DayContext> contexts = createDayContextsForWeek(sunday);
        for(DayContext dayContext: contexts) {
            Optional<DailyHours> existingHours = repo.findById(Date.valueOf(dayContext.getDate()));
            if(existingHours.isPresent()) {
                continue;
            }
            DailyHours hours = createHoursForDay(dayContext);
            log.info("Created hours: {}", hours);
            repo.save(hours);
        }

    }

    private DailyHours createHoursForDay(DayContext dayContext) {

        DailyHours hours = new DailyHours();
        hours.setDay(Date.valueOf(dayContext.getDate()));

        if(dayContext.isLeilYomKippurOrLeilTishaBav()) {
            hours.setClosed(true);
            return hours;
        }

        if(dayContext.isLeilShabbosOrLeilYomTov()) {
            hours.setClosed(false);
            LocalTime opening = dayContext.getCandleLighting().plusHours(1);
            hours.setOpening(Time.valueOf(opening));
            hours.setClosing(Time.valueOf(opening.plusMinutes(30)));
            return hours;
        }

        if(dayContext.isMotzeiYomKippur()) {
            hours.setClosed(false);
            LocalTime opening = dayContext.getTzeis().plusHours(1);
            hours.setOpening(Time.valueOf(opening));
            hours.setClosing(calculateClosing(opening));
            return hours;
        }

        if(dayContext.isLeilPurim()) {
            hours.setClosed(false);
            LocalTime opening = dayContext.getLatestTzeisForWeekRoundedUpToNearestFiveMinutes().plusHours(1);
            hours.setOpening(Time.valueOf(opening));
            hours.setClosing(calculateClosing(opening));
            return hours;
        }

        if(dayContext.isMotzeiShabbosOrMotzeiYomTov()) {
            hours.setClosed(false);
            LocalTime opening = dayContext.getTzeis().plusMinutes(45);
            hours.setOpening(Time.valueOf(opening));
            hours.setClosing(calculateClosing(opening));
            return hours;
        }

        hours.setClosed(false);
        LocalTime opening = dayContext.getLatestTzeisForWeekRoundedUpToNearestFiveMinutes();
        hours.setOpening(Time.valueOf(opening));
        hours.setClosing(calculateClosing(opening));
        return hours;

    }

    private Time calculateClosing(LocalTime opening) {
        LocalTime threeHoursAfterOpening = opening.plusHours(3);
        if(threeHoursAfterOpening.isAfter(LATEST_CLOSING_TIME)) {
            return Time.valueOf(LATEST_CLOSING_TIME);
        }

        // If it overflows into the next day
        if(threeHoursAfterOpening.getHour() < 12) {
            return Time.valueOf(LATEST_CLOSING_TIME);
        }
        return Time.valueOf(threeHoursAfterOpening);
    }

    private List<DayContext> createDayContextsForWeek(LocalDate sunday){
        LocalTime latestTzeis = getLatestTzaisForWeek(sunday);
        List<DayContext> contexts = new ArrayList<>(7);
        LocalDate currentDay = sunday;
        for(int i = 0; i < 7; i++) {
            currentDay = currentDay.plusDays(1);
            ComplexZmanimCalendar zmanimCalendar = createCalendar(currentDay);
            LocalDate nextDay = currentDay.plusDays(1);
            JewishCalendar jewishCalendarNextDay = new JewishCalendar(Date.from(nextDay.atStartOfDay(getTimezone()).toInstant()));
            JewishCalendar jewishCalendarCurrentDay = new JewishCalendar(Date.from(currentDay.atStartOfDay(getTimezone()).toInstant()));

            int currentDayYomTovIndex = jewishCalendarCurrentDay.getYomTovIndex();
            int nextDayYomTovIndex = jewishCalendarNextDay.getYomTovIndex();

            DayContext context = DayContext.builder()
                    .date(currentDay)
                    .latestTzeisForWeekRoundedUpToNearestFiveMinutes(latestTzeis)
                    .candleLighting(convertDateToLocalTime(zmanimCalendar.getCandleLighting()))
                    .isLeilShabbosOrLeilYomTov(isLeilYomTovOrShabbos(currentDay, jewishCalendarNextDay))
                    .isLeilPurim(nextDayYomTovIndex == JewishCalendar.PURIM)
                    .isLeilYomKippurOrLeilTishaBav(nextDayYomTovIndex == JewishCalendar.TISHA_BEAV || nextDayYomTovIndex == JewishCalendar.YOM_KIPPUR)
                    .isMotzeiShabbosOrMotzeiYomTov(currentDay.getDayOfWeek() == DayOfWeek.SATURDAY || YOM_TOV_INDEXES.contains(currentDayYomTovIndex))
                    .isMotzeiYomKippur(currentDayYomTovIndex == JewishCalendar.YOM_KIPPUR)
                    .tzeis(convertDateToLocalTime(zmanimCalendar.getTzais()))
                    .build();
            contexts.add(context);
        }
        return contexts;
    }

    private LocalTime convertDateToLocalTime(java.util.Date date) {
        return LocalDateTime.ofInstant(date.toInstant(), getTimezone()).toLocalTime().truncatedTo(ChronoUnit.MINUTES);
    }

    private LocalTime getLatestTzaisForWeek(LocalDate sunday) {

        LocalDate day = sunday;
        LocalTime latestTzais = LocalTime.MIN;

        for(int i = 0; i < 7; i++) {
            day = day.plusDays(1);
            ComplexZmanimCalendar zmanimCalendar = createCalendar(day);
            LocalTime tzais = zmanimCalendar.getTzais().toInstant().atZone(getTimezone()).toLocalTime();
            if(tzais.isAfter(latestTzais)) {
                latestTzais = tzais;
            }
        }

        // Round to next 5 minutes
        int minutesMod5 = latestTzais.getMinute() % 5;
        if(minutesMod5 != 0) {
            latestTzais = latestTzais.plusMinutes(5L - minutesMod5);
        }

        latestTzais = latestTzais.truncatedTo(ChronoUnit.MINUTES);

        return latestTzais;

    }

    private boolean isLeilYomTovOrShabbos(LocalDate date, JewishCalendar nextDay) {

        if(date.getDayOfWeek() == DayOfWeek.FRIDAY) {
            return true;
        }
        int yomTovIndex = nextDay.getYomTovIndex();
        return YOM_TOV_INDEXES.contains(yomTovIndex);

    }

    private ComplexZmanimCalendar createCalendar() {

        String locationName = "Los Angeles, CA";
        double latitude = 34.0549987; // Los Angeles, CA
        double longitude = -118.3969812; // Los Angeles, CA
        double elevation = 65; // optional elevation
        TimeZone timeZone = TimeZone.getTimeZone(config.getTimeZone());
        GeoLocation location = new GeoLocation(locationName, latitude, longitude, elevation, timeZone);
        return new ComplexZmanimCalendar(location);

    }

    private ComplexZmanimCalendar createCalendar(LocalDate day) {

        ComplexZmanimCalendar complexZmanimCalendar = createCalendar();
        complexZmanimCalendar.getCalendar().set(day.getYear(), day.getMonthValue() - 1, day.getDayOfMonth());
        return complexZmanimCalendar;

    }

    private ZoneId getTimezone(){
        if(timezone == null){
            timezone = ZoneId.of(config.getTimeZone());
        }
        return timezone;
    }
}
