package org.lamikvah.website.resource;

import lombok.extern.slf4j.Slf4j;
import org.lamikvah.website.data.*;
import org.lamikvah.website.exception.AppointmentCreationException;
import org.lamikvah.website.exception.UnauthorizedException;
import org.lamikvah.website.service.AppointmentService;
import org.lamikvah.website.service.MikvahUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Optional;

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

    @PostMapping("/appointments/{id}")
    public AppointmentCreationResponse editAppointment(@PathVariable("id") long id,
                                                       @RequestBody UpdateAppointmentRequest appointmentRequest,
                                                       HttpServletRequest servletRequest) {
        final MikvahUser user = mikvahUserService.getUser(servletRequest);

        if (!user.isAdmin()) {
            throw new UnauthorizedException("You are not authorized to edit appointments.");
        }
        try {
            final AppointmentSlotDto slot = appointmentService.editAppointment(id, appointmentRequest);
            return AppointmentCreationResponse.builder()
                    .slot(slot)
                    .message("Your appointment was modified successfully!")
                    .build();
        } catch (AppointmentCreationException e) {
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
