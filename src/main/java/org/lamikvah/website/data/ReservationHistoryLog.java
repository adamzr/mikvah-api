package org.lamikvah.website.data;

import java.time.LocalDateTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(indexes = { @Index(columnList = "mikvah_user_id", name = "rhl_mikvah_user_id_idx"),
        @Index(columnList = "created", name = "rhl_created_idx"),
        @Index(columnList = "stripe_id", name = "rhl_stripe_id_idx") })
public class ReservationHistoryLog {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long id;

    @ManyToOne
    @JoinColumn(name = "appointment_slot_id")
    private AppointmentSlot appointmentSlot;

    @ManyToOne
    @JoinColumn(name = "mikvah_user_id")
    private MikvahUser mikvahUser;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime created;

    @Enumerated(EnumType.STRING)
    private AppointmentAction action;

    @Column(name = "stripe_id")
    private String stripeId;

}
