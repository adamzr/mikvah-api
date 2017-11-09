package org.lamikvah.website.data;

import java.time.LocalDateTime;

import lombok.Value;

@Value
public class ReservationHistoryLog {

	private AppointmentSlot slot;
	private LocalDateTime created;
	private AppointmentAction action;
}
