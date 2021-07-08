package org.lamikvah.website.resource;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.lamikvah.website.data.UserDto;
import org.lamikvah.website.service.MikvahUserService;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Arrays;
import java.util.Collection;

import static org.hamcrest.CoreMatchers.is;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
public class UserControllerTest {
    private  static final String SomeUserId = "some-user-id";

    @Mock
    private MikvahUserService mikvahUserService;

    @InjectMocks
    private UserController subject;

    private MockMvc mvc;

    @BeforeEach
    public void setup() {
        JacksonTester.initFields(this, new ObjectMapper());
        mvc = MockMvcBuilders.standaloneSetup(subject).build();
    }

    @ParameterizedTest
    @MethodSource("givenAdminValue")
    public void getUserShouldExposeAdminPropertyFromServiceResponse(boolean givenAdminValue) throws Exception {
        when(mikvahUserService.getUserWithCreditCardInfo(SomeUserId)).thenReturn(UserDto.builder().admin(givenAdminValue).build());

        mvc.perform(MockMvcRequestBuilders
                .get("/user")
                .principal(() -> SomeUserId)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.admin", is(givenAdminValue)))
                .andReturn();
    }

    private static Collection<Boolean> givenAdminValue() {
        return Arrays.asList(true, false);
    }
}
