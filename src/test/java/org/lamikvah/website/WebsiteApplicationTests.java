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

    // @Autowired
    // private DailyHoursCreationService dhcs;
    //
    // @Autowired
    // private DailyHoursRepository repo;

    // @Test
    // public void generateHoursCSV() throws IOException {
    //
    // LocalDate start =
    // LocalDate.now().with(TemporalAdjusters.previous(DayOfWeek.SUNDAY));
    // dhcs.calculateHoursForWeek(start);
    // for (int i = 0; i < 53; i++) {
    // start = start.with(TemporalAdjusters.next(DayOfWeek.SUNDAY));
    // dhcs.calculateHoursForWeek(start);
    // }
    //
    // final File temp = File.createTempFile("hours", ".csv");
    // FileUtils.writeStringToFile(temp, "Date,Open,Closed",
    // StandardCharsets.UTF_8);
    // LocalDate date =
    // LocalDate.now().with(TemporalAdjusters.previous(DayOfWeek.SUNDAY));
    // for (int i = 0; i < 366; i++) {
    // date = date.plusDays(1);
    // final Optional<DailyHours> optionalHours = repo.findById(Date.valueOf(date));
    // final DailyHours hours = optionalHours.get();
    // if (hours != null) {
    // FileUtils.writeStringToFile(temp,
    // DateTimeFormatter.ISO_DATE.format(date)
    // + "," + (hours.getOpening() != null ? hours.getOpening().toString()
    // : "N/A")
    // + ","
    // + (hours.getClosing() != null ? hours.getClosing().toString()
    // : "N/A")
    // + "\n",
    // StandardCharsets.UTF_8, true);
    // // System.out.println(DateTimeFormatter.ISO_DATE.format(date) + "," +
    // // hours.getOpening().toString() + ","
    // // + hours.getClosing().toString());
    // }
    // }
    // System.out.println(temp.getAbsolutePath());
    // }

    @Test
    public void contextLoads() {

    }

}
