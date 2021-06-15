package org.lamikvah.website.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class AppointmentSlotCreationServiceTest {

    @Autowired
    private AppointmentSlotCreationService service;

    @Test
    public void testCreation() {

        service.createSlots();
    }
}
