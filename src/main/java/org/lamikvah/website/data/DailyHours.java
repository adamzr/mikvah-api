package org.lamikvah.website.data;

import java.time.LocalDate;
import java.time.LocalTime;

import lombok.Value;

@Value
public class DailyHours {

	private LocalDate day;
	private LocalTime opening;
	private LocalTime closing;
}
