package org.lamikvah.website.service;

import java.sql.Date;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.lamikvah.website.MikvahConfiguration;
import org.lamikvah.website.dao.AppointmentSlotRepository;
import org.lamikvah.website.dao.DailyHoursRepository;
import org.lamikvah.website.data.AppointmentSlot;
import org.lamikvah.website.data.DailyHours;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import lombok.extern.slf4j.Slf4j;
import net.sourceforge.zmanim.hebrewcalendar.JewishCalendar;

@Component
@Slf4j
public class AppointmentSlotCreationService {

    private static final int APPOINTMENT_LENGTH = 30;
    private static final long ONE_HOUR = 3600000;
    private static final long FIVE_MINUTES = 300000;

    private static final List<Integer> APPOINTMENT_ROOMS_OFFSETS = Arrays.asList(0, 15);

    @Autowired
    private AppointmentSlotRepository repo;

    @Autowired
    private DailyHoursRepository dailyHoursRepo;

    @Autowired
    private MikvahConfiguration config;

    @Scheduled(initialDelay = FIVE_MINUTES, fixedRate=ONE_HOUR)
    public void createSlots() {

        log.debug("Creating appointment slots.");

        LocalDate day = LocalDate.now();
        for(int i = 0; i < 14; i++) {
            day = day.plusDays(1);
            LocalDateTime start = day.atStartOfDay();
            LocalDateTime end = day.plusDays(1).atStartOfDay();
            List<AppointmentSlot> slots = repo.findByStartBetweenOrderByStartAsc(start, end);
            if(CollectionUtils.isEmpty(slots)) {
                Optional<DailyHours> hoursOptional = dailyHoursRepo.findById(Date.valueOf(day));
                if(hoursOptional.isPresent()) {
                    DailyHours hours = hoursOptional.get();
                    if(hours.isClosed() || isLeilYomTovOrShabbos(day)) {
                        continue;
                    }
                    for(int offset: APPOINTMENT_ROOMS_OFFSETS) {
                        createAppointments(day, hours, offset);
                    }
                }
            }
        }

    }

    private void createAppointments(LocalDate day, DailyHours hours, int offset) {

        LocalTime appointmentStart = hours.getOpening().toLocalTime().plusMinutes(offset);
        while(!wouldEndAfterClosing(appointmentStart, hours)) {
            AppointmentSlot slot = new AppointmentSlot();
            slot.setStart(LocalDateTime.of(day, appointmentStart));
            log.info("Created appointment slot {}", slot);
            repo.save(slot);
            appointmentStart = appointmentStart.plusMinutes(APPOINTMENT_LENGTH);
        }

    }

    private boolean wouldEndAfterClosing(LocalTime appointmentStart, DailyHours hours) {

        LocalTime appointmentEnd = appointmentStart.plusMinutes(APPOINTMENT_LENGTH);
        return appointmentEnd.isAfter(hours.getClosing().toLocalTime());

    }

    private boolean isLeilYomTovOrShabbos(LocalDate date) {

        JewishCalendar nextDay = new JewishCalendar(Date.from(date.plusDays(1).atStartOfDay(ZoneId.of(config.getTimeZone())).toInstant()));
        if(date.getDayOfWeek() == DayOfWeek.FRIDAY) {
            return true;
        }
        int yomTovIndex = nextDay.getYomTovIndex();
        return DailyHoursCreationService.YOM_TOV_INDEXES.contains(yomTovIndex);

    }
}
