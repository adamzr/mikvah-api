package org.lamikvah.website.service;

import java.sql.Date;
import java.sql.Time;
import java.time.DayOfWeek;
import java.time.Duration;
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

    /**
     * Mikvah should generally not be close before 9:30 PM
     */
    private static final LocalTime EARLIEST_CLOSING_TIME = LocalTime.of(21, 30);

    /**
     * Mikvah should always be open for at least 2 hours
     */
    private static final Duration MINIMUM_OPEN_DURARTION = Duration.ofHours(2);

    @Autowired
    private DailyHoursRepository repo;

    @Autowired
    private MikvahConfiguration config;

    @Scheduled(initialDelay = 0, fixedRate = ONE_HOUR)
    public void createHoursForNext3Weeks() {

        log.debug("Calculating hours for next 3 weeks.");

        LocalDate start = LocalDate.now().with(TemporalAdjusters.previous(DayOfWeek.SUNDAY));
        calculateHoursForWeek(start);
        for (int i = 0; i < 2; i++) {
            start = start.with(TemporalAdjusters.next(DayOfWeek.SUNDAY));
            calculateHoursForWeek(start);
        }

    }

    public void calculateHoursForWeek(final LocalDate sunday) {

        log.debug("Calculating hours for the week starting on {}", sunday);

        final List<DayContext> contexts = createDayContextsForWeek(sunday);
        for (final DayContext dayContext : contexts) {
            final Optional<DailyHours> existingHours = repo.findById(Date.valueOf(dayContext.getDate()));
            final DailyHours hours = createHoursForDay(dayContext);
            if (existingHours.isPresent()) {
                final DailyHours existing = existingHours.get();
                if (existing.equals(hours)) {
                    continue;
                } else {
                    repo.delete(existing);
                }
            }
            log.info("Created hours: {}", hours);
            repo.save(hours);
        }

    }

    private DailyHours createHoursForDay(final DayContext dayContext) {

        final DailyHours hours = new DailyHours();
        hours.setDay(Date.valueOf(dayContext.getDate()));

        if (dayContext.isLeilYomKippurOrLeilTishaBav()) {
            hours.setClosed(true);
            return hours;
        }

        if (dayContext.isLeilShabbosOrLeilYomTov()) {
            hours.setClosed(false);
            final LocalTime opening = roundToNextTimeEndingIn5or0(dayContext.getCandleLighting().plusHours(1));
            hours.setOpening(Time.valueOf(opening));
            hours.setClosing(Time.valueOf(opening.plusMinutes(30)));
            return hours;
        }

        if (dayContext.isMotzeiYomKippur()) {
            hours.setClosed(false);
            final LocalTime opening = roundToNextTimeEndingIn5or0(dayContext.getTzeis().plusHours(1));
            hours.setOpening(Time.valueOf(opening));
            hours.setClosing(calculateClosing(opening));
            return hours;
        }

        if (dayContext.isLeilPurim()) {
            hours.setClosed(false);
            final LocalTime opening = roundToNextTimeEndingIn5or0(
                    dayContext.getLatestTzeisForWeekRoundedUpToNearestFiveMinutes().plusHours(1));
            hours.setOpening(Time.valueOf(opening));
            hours.setClosing(calculateClosing(opening));
            return hours;
        }

        if (dayContext.isMotzeiTishaBav()) {
            hours.setClosed(false);
            final LocalTime opening = roundToNextTimeEndingIn5or0(
                    dayContext.getLatestTzeisForWeekRoundedUpToNearestFiveMinutes().plusHours(1));
            hours.setOpening(Time.valueOf(opening));
            hours.setClosing(calculateClosing(opening));
            return hours;
        }

        if (dayContext.isMotzeiShabbosOrMotzeiYomTov()) {
            hours.setClosed(false);
            final LocalTime opening = roundToNextTimeEndingIn5or0(dayContext.getTzeis().plusMinutes(45));
            hours.setOpening(Time.valueOf(opening));
            hours.setClosing(calculateClosing(opening));
            return hours;
        }

        hours.setClosed(false);
        final LocalTime opening = dayContext.getLatestTzeisForWeekRoundedUpToNearestFiveMinutes();
        hours.setOpening(Time.valueOf(opening));
        hours.setClosing(calculateClosing(opening));
        return hours;

    }

    private Time calculateClosing(final LocalTime opening) {

        final LocalTime regularClosingTime = opening.plusHours(3);
        final LocalTime earlyClosingTime = opening.plusHours(2);

        LocalTime closingTime = regularClosingTime;

        // If regular closing time would be after 11pm
        if (regularClosingTime.isAfter(LATEST_CLOSING_TIME)) {
            // then if 11pm would make the mikvah open for less than 2 hours
            if (Duration.between(opening, LATEST_CLOSING_TIME).compareTo(MINIMUM_OPEN_DURARTION) < 0) {
                closingTime = earlyClosingTime; // close 2 hours after opening
            } else {
                closingTime = LATEST_CLOSING_TIME; // otherwise, close at 11pm
            }
        }

        // If it overflows into the next day
        if (closingTime.getHour() < 12)
            return Time.valueOf(LocalTime.of(23, 59));// just before midnight

        // don't close before 9:30 pm
        if (closingTime.isBefore(EARLIEST_CLOSING_TIME))
            return Time.valueOf(EARLIEST_CLOSING_TIME);

        return Time.valueOf(closingTime);
    }

    private List<DayContext> createDayContextsForWeek(final LocalDate sunday) {

        final LocalTime latestTzeis = getLatestTzaisForWeek(sunday);
        final List<DayContext> contexts = new ArrayList<>(7);
        for (int i = 0; i < 7; i++) {
            final LocalDate currentDay = sunday.plusDays(i);
            final ComplexZmanimCalendar zmanimCalendar = createCalendar(currentDay);
            final LocalDate nextDay = currentDay.plusDays(1);
            final JewishCalendar jewishCalendarNextDay = new JewishCalendar(
                    java.util.Date.from(nextDay.atStartOfDay(getTimezone()).toInstant()));
            final JewishCalendar jewishCalendarCurrentDay = new JewishCalendar(
                    java.util.Date.from(currentDay.atStartOfDay(getTimezone()).toInstant()));

            final int currentDayYomTovIndex = jewishCalendarCurrentDay.getYomTovIndex();
            final int nextDayYomTovIndex = jewishCalendarNextDay.getYomTovIndex();

            final DayContext context = DayContext.builder()
                    .date(currentDay)
                    .latestTzeisForWeekRoundedUpToNearestFiveMinutes(latestTzeis)
                    .candleLighting(convertDateToLocalTime(zmanimCalendar.getCandleLighting()))
                    .isLeilShabbosOrLeilYomTov(isLeilYomTovOrShabbos(currentDay, jewishCalendarNextDay))
                    .isLeilPurim(nextDayYomTovIndex == JewishCalendar.PURIM)
                    .isLeilYomKippurOrLeilTishaBav(nextDayYomTovIndex == JewishCalendar.TISHA_BEAV
                            || nextDayYomTovIndex == JewishCalendar.YOM_KIPPUR)
                    .isMotzeiShabbosOrMotzeiYomTov(currentDay.getDayOfWeek() == DayOfWeek.SATURDAY
                            || YOM_TOV_INDEXES.contains(currentDayYomTovIndex))
                    .isMotzeiYomKippur(currentDayYomTovIndex == JewishCalendar.YOM_KIPPUR)
                    .isMotzeiTishaBav(currentDayYomTovIndex == JewishCalendar.TISHA_BEAV)
                    .tzeis(convertDateToLocalTime(zmanimCalendar.getTzais()))
                    .build();
            contexts.add(context);
        }
        return contexts;
    }

    private LocalTime convertDateToLocalTime(final java.util.Date date) {

        return LocalDateTime.ofInstant(date.toInstant(), getTimezone()).toLocalTime().truncatedTo(ChronoUnit.MINUTES);
    }

    private LocalTime getLatestTzaisForWeek(final LocalDate sunday) {

        LocalTime latestTzais = LocalTime.MIN;

        for (int i = 0; i < 7; i++) {
            final LocalDate day = sunday.plusDays(i);
            final ComplexZmanimCalendar zmanimCalendar = createCalendar(day);
            final LocalTime tzais = zmanimCalendar.getTzais().toInstant().atZone(getTimezone()).toLocalTime();
            if (tzais.isAfter(latestTzais)) {
                latestTzais = tzais;
            }
        }

        latestTzais = roundToNextTimeEndingIn5or0(latestTzais);

        return latestTzais;

    }

    private LocalTime roundToNextTimeEndingIn5or0(final LocalTime time) {

        LocalTime adjusted = time;
        final int minutesMod5 = time.getMinute() % 5;
        if (minutesMod5 != 0) {
            adjusted = time.plusMinutes(5L - minutesMod5);
        }

        adjusted = adjusted.truncatedTo(ChronoUnit.MINUTES);
        return adjusted;
    }

    private boolean isLeilYomTovOrShabbos(final LocalDate date, final JewishCalendar nextDay) {

        if (date.getDayOfWeek() == DayOfWeek.FRIDAY)
            return true;
        final int yomTovIndex = nextDay.getYomTovIndex();
        return YOM_TOV_INDEXES.contains(yomTovIndex);

    }

    private ComplexZmanimCalendar createCalendar() {

        final String locationName = "Los Angeles, CA";
        final double latitude = 34.0549987; // Los Angeles, CA
        final double longitude = -118.3969812; // Los Angeles, CA
        final double elevation = 65; // optional elevation
        final TimeZone timeZone = TimeZone.getTimeZone(config.getTimeZone());
        final GeoLocation location = new GeoLocation(locationName, latitude, longitude, elevation, timeZone);
        return new ComplexZmanimCalendar(location);

    }

    private ComplexZmanimCalendar createCalendar(final LocalDate day) {

        final ComplexZmanimCalendar complexZmanimCalendar = createCalendar();
        complexZmanimCalendar.getCalendar().set(day.getYear(), day.getMonthValue() - 1, day.getDayOfMonth());
        return complexZmanimCalendar;

    }

    private ZoneId getTimezone() {

        if (timezone == null) {
            timezone = ZoneId.of(config.getTimeZone());
        }
        return timezone;
    }
}
