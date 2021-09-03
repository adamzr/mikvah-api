package org.lamikvah.website.resource;

import java.security.Principal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import com.auth0.exception.Auth0Exception;
import org.lamikvah.website.MikvahConfiguration;
import org.lamikvah.website.data.AdminAppointmentView;
import org.lamikvah.website.data.AppointmentsViewRequest;
import org.lamikvah.website.data.AttendentAppointmentView;
import org.lamikvah.website.data.MikvahUser;
import org.lamikvah.website.exception.ServerErrorException;
import org.lamikvah.website.exception.UnauthorizedException;
import org.lamikvah.website.service.AppointmentService;
import org.lamikvah.website.service.MikvahUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import lombok.extern.slf4j.Slf4j;

import javax.servlet.http.HttpServletRequest;

@CrossOrigin
@RestController
@Slf4j
public class AttendentsController {

    @Autowired AppointmentService appointmentService;
    @Autowired MikvahUserService mikvahUserService;
    @Autowired MikvahConfiguration config;

    @GetMapping("/attendent-daily-list")
    public List<AttendentAppointmentView> dailyList(){
        LocalDateTime now = LocalDateTime.now(Clock.system(ZoneId.of(config.getTimeZone())));
        log.info("Getting attendants view for {}", now);
        return appointmentService.getAppointmentsForAttendants(now);
    }

    @PostMapping("/admin-daily-list")
    public List<AdminAppointmentView> dailyList(@RequestBody AppointmentsViewRequest request,
                                                HttpServletRequest servletRequest) {
        Principal principal = servletRequest.getUserPrincipal();

        try {
            final MikvahUser user = mikvahUserService.getUser(principal.getName());

            if (!user.isAdmin()) {
                throw new UnauthorizedException("You are not authorized to view this resource.");
            }
        } catch (Auth0Exception e) {
            log.error("Failed to get user from Auth0.", e);
            throw new ServerErrorException("There was a problem getting your user information. Please try again later.", e);
        }

        LocalDate date = request.getDate();
        log.info("Getting admin view for {}", date);
        return appointmentService.getAppointmentsForAdmins(date);
    }
}
