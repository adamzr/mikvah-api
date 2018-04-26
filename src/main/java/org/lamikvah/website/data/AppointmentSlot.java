package org.lamikvah.website.data;

import java.time.LocalDateTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(indexes= {
        @Index(columnList="start", name="appointment_slot_start_idx"),
        @Index(columnList="mikvah_user_id", name="appointment_slot_mikvah_user_id_idx")})
public class AppointmentSlot {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Column(name="start")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime start;

    @ManyToOne(optional=true)
    @JoinColumn(name="mikvah_user_id")
    private MikvahUser mikvahUser;

    @Column(name="stripe_charge_id")
    private String stripeChargeId;

    @Column(name="notes")
    private String notes;

}
