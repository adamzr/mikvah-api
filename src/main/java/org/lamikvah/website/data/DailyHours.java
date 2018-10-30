package org.lamikvah.website.data;

import java.sql.Date;
import java.sql.Time;
import java.time.LocalTime;
import java.util.Optional;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Table;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(indexes= {@Index(columnList="day", name="daily_hours_day_idx")})
public class DailyHours {

    @Id
    private Date day;

    private Time opening;

    private Time closing;

    private boolean closed;

    @JsonIgnore
    public Optional<LocalTime> getOpeningLocalTime() {
        
        if(opening == null) {
            return Optional.empty();
        }

        return Optional.ofNullable(opening.toLocalTime());

    }

    @JsonIgnore
    public Optional<LocalTime> getClosingLocalTime() {
        
        if(closing == null) {
            return Optional.empty();
        }

        return Optional.ofNullable(closing.toLocalTime());

    }
}
