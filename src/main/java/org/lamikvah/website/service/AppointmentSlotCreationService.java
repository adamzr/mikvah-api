package org.lamikvah.website.service;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.sql.Date;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.lamikvah.website.MikvahConfiguration;
import org.lamikvah.website.dao.AppointmentSlotRepository;
import org.lamikvah.website.dao.DailyHoursRepository;
import org.lamikvah.website.data.AppointmentSlot;
import org.lamikvah.website.data.DailyHours;
import org.lamikvah.website.data.RoomType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import net.sourceforge.zmanim.hebrewcalendar.JewishCalendar;

@Component
@Slf4j
public class AppointmentSlotCreationService {

  private static final Map<RoomType, Duration> ROOM_TYPE_TO_APPOINTMENT_LENGTH =
      ImmutableMap.of(RoomType.SHOWER,
          Duration.ofMinutes(30), RoomType.BATH, Duration.ofMinutes(75));

  private static final long ONE_HOUR_IN_MILLSECONDS = 3600000;

  private static final long ONE_MINUTE_IN_MILLSECONDS = 60000;

  private static final Map<RoomType,
      List<Integer>> APPOINTMENT_ROOM_TYPE_TO_LIST_OF_START_TIME_OFFSETS = ImmutableMap
          .of(RoomType.SHOWER, ImmutableList.of(25, 25, 30, 30, 35), // 5 showers
              RoomType.BATH, ImmutableList.of(0, 0, 5, 5, 10, 10, 10)); // 7 baths

  @Autowired
  private AppointmentSlotRepository repo;

  @Autowired
  private DailyHoursRepository dailyHoursRepo;

  @Autowired
  private MikvahConfiguration config;

  @Scheduled(initialDelay = ONE_MINUTE_IN_MILLSECONDS, fixedRate = ONE_HOUR_IN_MILLSECONDS)
  public void createSlots() {

    log.debug("Creating appointment slots.");

    LocalDate day = LocalDate.now();
    for (int i = 0; i < 14; i++) {
      day = day.plusDays(1);
      final LocalDateTime start = day.atStartOfDay();
      final LocalDateTime end = day.plusDays(1).atStartOfDay();
      for (final RoomType roomType : APPOINTMENT_ROOM_TYPE_TO_LIST_OF_START_TIME_OFFSETS.keySet()) {
        final List<LocalDateTime> existingStartTimes = repo
            .findByStartBetweenAndRoomTypeOrderByStartAsc(start, end,
                roomType)
            .stream().map(AppointmentSlot::getStart).collect(Collectors.toList());
        if (!existingStartTimes.isEmpty()) {
          continue;
        }
        final Optional<DailyHours> hoursOptional = dailyHoursRepo.findById(Date.valueOf(day));
        if (hoursOptional.isPresent()) {
          final DailyHours hours = hoursOptional.get();
          if (hours.isClosed() || isLeilYomTovOrShabbos(day)) {
            continue;
          }
          for (final int offset : APPOINTMENT_ROOM_TYPE_TO_LIST_OF_START_TIME_OFFSETS
              .get(roomType)) {
            createAppointments(roomType, day, hours, offset, existingStartTimes);
          }
        }

      }
    }

  }

  private void createAppointments(final RoomType roomType, final LocalDate day,
      final DailyHours hours,
      final int offset, final List<LocalDateTime> existingStartTimes) {

    LocalDateTime appointmentStart =
        LocalDateTime.of(day, hours.getOpening().toLocalTime().plusMinutes(offset));
    final LocalDateTime closing =
        LocalDateTime.of(hours.getDay().toLocalDate(), hours.getClosing().toLocalTime());
    while (!wouldEndAfterClosing(roomType, appointmentStart, closing)) {
      final AppointmentSlot slot = new AppointmentSlot();
      slot.setStart(appointmentStart);
      slot.setRoomType(roomType);
      if (!existingStartTimes.contains(appointmentStart)) {
        // If we have not already created this appointment in a previous run
        log.info("Created appointment slot {}", slot);
        repo.save(slot);
      }
      appointmentStart = appointmentStart.plus(ROOM_TYPE_TO_APPOINTMENT_LENGTH.get(roomType));
    }

  }

  private boolean wouldEndAfterClosing(final RoomType roomType,
      final LocalDateTime appointmentStart,
      final LocalDateTime closing) {

    final LocalDateTime appointmentEnd =
        appointmentStart.plus(ROOM_TYPE_TO_APPOINTMENT_LENGTH.get(roomType));
    return appointmentEnd.isAfter(closing);

  }

  private boolean isLeilYomTovOrShabbos(final LocalDate date) {

    final JewishCalendar nextDay = new JewishCalendar(
        java.util.Date
            .from(date.plusDays(1).atStartOfDay(ZoneId.of(config.getTimeZone())).toInstant()));
    if (date.getDayOfWeek() == DayOfWeek.FRIDAY) {
      return true;
    }
    final int yomTovIndex = nextDay.getYomTovIndex();
    return DailyHoursCreationService.YOM_TOV_INDEXES.contains(yomTovIndex);

  }
}
