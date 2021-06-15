package org.lamikvah.website.service;

import static org.junit.Assert.assertEquals;

import java.sql.Time;
import java.time.LocalTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class DailyHoursCreationServiceTest {

    @Autowired
    private DailyHoursCreationService service;

    @Test
    public void testHours() {

        final Time closingTime = service.calculateClosing(LocalTime.of(12 + 9, 25));
        assertEquals(Time.valueOf(LocalTime.of(12 + 9 + 2, 25)), closingTime);
    }
}
