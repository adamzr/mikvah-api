package org.lamikvah.website;

//import java.time.DayOfWeek;
//import java.time.LocalDate;
//import java.time.temporal.TemporalAdjusters;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
//import org.lamikvah.website.service.DailyHoursCreationService;
//import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
public class WebsiteApplicationTests {
    
//    @Autowired private DailyHoursCreationService dhcs;
    
//    @Test
//    public void generateHoursCSV() {
//        LocalDate start = LocalDate.now().with(TemporalAdjusters.previous(DayOfWeek.SUNDAY));
//        dhcs.calculateHoursForWeek(start);
//        for(int i = 0; i < 53; i++) {
//            start = start.with(TemporalAdjusters.next(DayOfWeek.SUNDAY));
//            dhcs.calculateHoursForWeek(start);
//        }
//    }

	@Test
	@Ignore
	public void contextLoads() {
	}

}
