package org.lamikvah.website.resource;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.lamikvah.website.data.AppointmentCreationResponse;
import org.lamikvah.website.data.AppointmentRequest;
import org.lamikvah.website.data.AppointmentSlotDto;
import org.lamikvah.website.data.MikvahUser;
import org.lamikvah.website.exception.AppointmentCreationException;
import org.lamikvah.website.exception.ServerErrorException;
import org.lamikvah.website.service.AppointmentService;
import org.lamikvah.website.service.MikvahUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.auth0.exception.Auth0Exception;

import lombok.extern.slf4j.Slf4j;

@CrossOrigin
@RestController
@Slf4j
public class AppointmentController {


    @Autowired
    private AppointmentService appointmentService;
    
    @Autowired
    private MikvahUserService mikvahUserService;

    @GetMapping(value = "/test-auth")
    public String testAuth(HttpServletRequest request) {
        Principal principal = request.getUserPrincipal();
        return principal.getName();
    }

    @GetMapping(value = "/test-no-auth")
    public String testNoAuth() {
        return "Hello, World!";
    }

    @GetMapping("/appointments/availability")
    public List<LocalDateTime> getAvailableAppointments() {
        return appointmentService.getAvailableTimes();
    }

    @PostMapping("/appointments")
    public AppointmentCreationResponse createAppointment(@RequestBody AppointmentRequest appointmentRequest, HttpServletRequest request) {
        MikvahUser user;
        try {
            user = mikvahUserService.getUser(request);
        } catch (Auth0Exception e) {
            throw new ServerErrorException("There was a problem getting your user information. Please try again later.",
                    e);
        }
        try {
            AppointmentSlotDto slot = appointmentService.createAppointment(appointmentRequest,
                    user);
            return AppointmentCreationResponse.builder()
                    .slot(slot)
                    .message("Your appointment was created successfully!")
                    .build();
        } catch (AppointmentCreationException e) {
            return AppointmentCreationResponse.builder()
                    .message(e.getMessage())
                    .build();
        }
    }
    
    @DeleteMapping("/appointments/{id}")
    public String cancelAppointment(@PathVariable("id") Long id, HttpServletRequest request) {
        MikvahUser user;
        try {
            user = mikvahUserService.getUser(request);
        } catch (Auth0Exception e) {
            throw new ServerErrorException("There was a problem getting your user information. Please try again later.",
                    e);
        }
        appointmentService.cancelAppointment(user, id);
        log.info("Cancelled appointment " + id);
        return "\"Cancelled appointment " + id + "\"";
    }

}
