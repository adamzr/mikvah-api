package org.lamikvah.website.resource;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import org.lamikvah.website.MikvahConfiguration;
import org.lamikvah.website.data.AttendentAppointmentView;
import org.lamikvah.website.service.AppointmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.extern.slf4j.Slf4j;

@CrossOrigin
@RestController
@Slf4j
public class AttendentsController {

    @Autowired AppointmentService service;
    @Autowired MikvahConfiguration config;

    @GetMapping("/attendent-daily-list")
    public List<AttendentAppointmentView> dailyList(){
        LocalDateTime now = LocalDateTime.now(Clock.system(ZoneId.of(config.getTimeZone())));
        log.info("Getting attendants view for {}", now);
        return service.getAppointmentsForAttendants(now);
    }
}
