package org.lamikvah.website.resource;

import com.auth0.exception.Auth0Exception;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.lamikvah.website.MikvahConfiguration;
import org.lamikvah.website.data.AdminAppointmentView;
import org.lamikvah.website.data.AppointmentsViewRequest;
import org.lamikvah.website.data.MikvahUser;
import org.lamikvah.website.service.AppointmentService;
import org.lamikvah.website.service.MikvahUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({AttendentsController.class, MikvahConfiguration.class})
@ActiveProfiles("test")
class AttendentsControllerTest {
    private static final String SomeUserId = "some-user-id";
    private static final LocalDate SomeDate = LocalDate.of(2000, 2, 2);

    @MockBean
    private AppointmentService appointmentService;
    @MockBean
    private MikvahUserService mikvahUserService;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired private MockMvc mvc;

    // This object will be magically initialized by the initFields method below.
    JacksonTester<AppointmentsViewRequest> jsonRequest;
    JacksonTester<List<AdminAppointmentView>> jsonResponse;

    @BeforeEach
    public void setup() {
        JacksonTester.initFields(this, objectMapper);
    }

    @Test
    @WithMockUser(username = SomeUserId)
    public void adminDailyListShouldReturnUnauthorizedIfRequestingUserIsNotAdmin() throws Exception {
        when(mikvahUserService.getUser(SomeUserId)).thenReturn(MikvahUser.builder().admin(false).build());

        mvc.perform(MockMvcRequestBuilders
                .post("/admin-daily-list")
                .content(jsonRequest.write(new AppointmentsViewRequest()).getJson())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andReturn();
    }

    @Test
    @WithMockUser(username = SomeUserId)
    public void adminDailyListShouldReturnServerErrorIfEncountersAuth0Exception() throws Exception {
        when(mikvahUserService.getUser(SomeUserId)).thenThrow(new Auth0Exception("Purposely thrown from test."));

        mvc.perform(MockMvcRequestBuilders
                .post("/admin-daily-list")
                .content(jsonRequest.write(new AppointmentsViewRequest()).getJson())
                .principal(() -> SomeUserId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError())
                .andReturn();
    }

    @Test
    @WithMockUser(username = SomeUserId)
    public void adminDailyListShouldReturn200WithAppointmentsForGivenDateWhenRequestingUserIsAdmin() throws Exception {
        final AdminAppointmentView appointment1 = AdminAppointmentView.builder()
                .email("some-email@test.org").firstName("First").time("1:00 PM").build();
        final AdminAppointmentView appointment2 = AdminAppointmentView.builder()
                .email("another-email@test.org").firstName("Second").time("2:00 PM").build();
        final AdminAppointmentView appointment3 = AdminAppointmentView.builder()
                .email("third-email@test.org").firstName("Third").time("3:00 PM").build();
        when(mikvahUserService.getUser(SomeUserId)).thenReturn(MikvahUser.builder().admin(true).build());
        when(appointmentService.getAppointmentsForAdmins(SomeDate))
                .thenReturn(Arrays.asList(appointment2, appointment1, appointment3));

        final MockHttpServletResponse response = mvc.perform(MockMvcRequestBuilders
                .post("/admin-daily-list")
                .content(jsonRequest.write(new AppointmentsViewRequest(SomeDate)).getJson())
                .contentType(MediaType.APPLICATION_JSON))
                .andReturn()
                .getResponse();

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(jsonResponse.parseObject(response.getContentAsString()))
                .containsExactly(appointment2, appointment1, appointment3);
    }
}
