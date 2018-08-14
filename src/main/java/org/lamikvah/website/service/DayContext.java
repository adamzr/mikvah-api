package org.lamikvah.website.service;

import java.time.LocalDate;
import java.time.LocalTime;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class DayContext {

    LocalDate date;
    LocalTime latestTzeisForWeekRoundedUpToNearestFiveMinutes;
    LocalTime tzeis;
    LocalTime candleLighting;
    boolean isLeilShabbosOrLeilYomTov;
    boolean isMotzeiShabbosOrMotzeiYomTov;
    boolean isMotzeiYomKippur;
    boolean isMotzeiTishaBav;
    boolean isLeilYomKippurOrLeilTishaBav;
    boolean isLeilPurim;

}
