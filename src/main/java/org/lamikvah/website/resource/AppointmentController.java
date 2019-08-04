package org.lamikvah.website.resource;

import java.util.List;
import java.util.Optional;

import javax.servlet.http.HttpServletRequest;

import org.lamikvah.website.data.AppointmentCreationResponse;
import org.lamikvah.website.data.AppointmentRequest;
import org.lamikvah.website.data.AppointmentSlotDto;
import org.lamikvah.website.data.AvailableDateTimeAndRoomType;
import org.lamikvah.website.data.MikvahUser;
import org.lamikvah.website.exception.AppointmentCreationException;
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

import lombok.extern.slf4j.Slf4j;

@CrossOrigin
@RestController
@Slf4j
public class AppointmentController {

    @Autowired
    private AppointmentService appointmentService;

    @Autowired
    private MikvahUserService mikvahUserService;

    @GetMapping("/appointments/availability")
    public List<AvailableDateTimeAndRoomType> getAvailableAppointments() {

        return appointmentService.getAvailableTimes();
    }

    @PostMapping("/appointments")
    public AppointmentCreationResponse createAppointment(@RequestBody final AppointmentRequest appointmentRequest,
            final HttpServletRequest request) {

        final MikvahUser user = mikvahUserService.getUser(request);
        try {
            final AppointmentSlotDto slot = appointmentService.createAppointment(appointmentRequest,
                    user);
            return AppointmentCreationResponse.builder()
                    .slot(slot)
                    .message("Your appointment was created successfully!")
                    .build();
        } catch (final AppointmentCreationException e) {
            return AppointmentCreationResponse.builder()
                    .message(e.getMessage())
                    .build();
        }
    }

    @DeleteMapping("/appointments/{id}")
    public String cancelAppointment(@PathVariable("id") final Long id, final HttpServletRequest request) {

        final MikvahUser user = mikvahUserService.getUser(request);
        final Optional<String> refundId = appointmentService.cancelAppointment(user, id);
        log.info("Cancelled appointment {}.", id);
        if (refundId.isPresent())
            return "\"Your appointment was cancelled and your fee was refunded. Thank you.\"";
        else
            return "\"Your appointment was cancelled. Thank you.\"";

    }

}
