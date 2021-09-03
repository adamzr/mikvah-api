package org.lamikvah.website.service;

import org.junit.jupiter.api.Test;
import org.lamikvah.website.dao.AppointmentSlotRepository;
import org.lamikvah.website.data.AdminAppointmentView;
import org.lamikvah.website.data.AppointmentSlot;
import org.lamikvah.website.data.MikvahUser;
import org.lamikvah.website.data.RoomType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class AppointmentServiceTest {
    private static final LocalDate SomeDate = LocalDate.of(2000, 12, 1);

    @MockBean
    AppointmentSlotRepository appointmentSlotRepository;

    @Autowired AppointmentService subject;

    @Test
    public void getAppointmentsForAdminsShouldReturnAppointmentsForGivenDateSortedByTimeAscending() {
        final String someRoomType = "bath";
        final String anotherRoomType = "shower";

        final MikvahUser someUser = MikvahUser.builder().id(1L).title("some title").firstName("some-first")
                .lastName("some-last").email("some-email@example.com").phoneNumber("1234567890").build();
        final MikvahUser anotherUser = MikvahUser.builder().id(2L).title("another title").firstName("another-first")
                .lastName("another-last").email("another-email@example.com").phoneNumber("0987654321").build();
        final AppointmentSlot slot1 = new AppointmentSlot(111L,
                LocalDateTime.of(SomeDate, LocalTime.of(1, 10)), someUser, null,
                "some notes", RoomType.valueOf(someRoomType.toUpperCase()));
        final AppointmentSlot slot2 = new AppointmentSlot(222L,
                LocalDateTime.of(SomeDate, LocalTime.of(2, 20)), anotherUser, null,
                "another notes", RoomType.valueOf(anotherRoomType.toUpperCase()));

        when(appointmentSlotRepository.findByStartBetweenOrderByStartAsc(any(), any()))
                .thenReturn(Arrays.asList(slot1, slot2));

        final List<AdminAppointmentView> response = subject.getAppointmentsForAdmins(SomeDate);

        verify(appointmentSlotRepository)
                .findByStartBetweenOrderByStartAsc(LocalDateTime.of(SomeDate, LocalTime.MIN), LocalDateTime.of(SomeDate, LocalTime.MAX));
        assertThat(response)
                .extracting(AdminAppointmentView::getTitle, AdminAppointmentView::getFirstName, AdminAppointmentView::getLastName,
                        AdminAppointmentView::getEmail, AdminAppointmentView::getPhoneNumber, AdminAppointmentView::getTime,
                        AdminAppointmentView::getRoomType, AdminAppointmentView::getNotes)
                .containsExactly(
                        tuple(someUser.getTitle(), someUser.getFirstName(), someUser.getLastName(), someUser.getEmail(),
                                someUser.getPhoneNumber(), "1:10 AM", someRoomType, slot1.getNotes()),
                        tuple(anotherUser.getTitle(), anotherUser.getFirstName(), anotherUser.getLastName(), anotherUser.getEmail(),
                                anotherUser.getPhoneNumber(), "2:20 AM", anotherRoomType, slot2.getNotes()));
    }
}
