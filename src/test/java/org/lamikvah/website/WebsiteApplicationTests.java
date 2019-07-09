package org.lamikvah.website;

import org.junit.jupiter.api.Test;

// import java.time.DayOfWeek;
// import java.time.LocalDate;
// import java.time.temporal.TemporalAdjusters;

// import org.lamikvah.website.service.DailyHoursCreationService;
// import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class WebsiteApplicationTests {

    // @Autowired private DailyHoursCreationService dhcs;

    // @Test
    // public void generateHoursCSV() {
    // LocalDate start =
    // LocalDate.now().with(TemporalAdjusters.previous(DayOfWeek.SUNDAY));
    // dhcs.calculateHoursForWeek(start);
    // for(int i = 0; i < 53; i++) {
    // start = start.with(TemporalAdjusters.next(DayOfWeek.SUNDAY));
    // dhcs.calculateHoursForWeek(start);
    // }
    // }

    @Test
    public void contextLoads() {

    }

}
