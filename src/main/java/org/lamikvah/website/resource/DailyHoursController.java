package org.lamikvah.website.resource;

import java.util.List;

import org.lamikvah.website.data.DailyHours;
import org.lamikvah.website.service.DailyHoursService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@CrossOrigin
@RestController
public class DailyHoursController {
    
    @Autowired private DailyHoursService service;

    @GetMapping("/hours")
    public List<DailyHours> getHours() {
        
        return service.getHoursForCurrentWeek();
        
    }
}
