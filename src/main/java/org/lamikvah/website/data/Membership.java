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
import javax.persistence.OneToOne;
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
@Table(indexes= {@Index(columnList="mikvah_user_id", name="membership_mikvah_user_id_idx"),
        @Index(columnList="expiration", name="membership_expiration_idx"),
        @Index(columnList="stripe_subscription_id", name="membership_stripe_subscription_id_idx")})
public class Membership {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private long id;

    @OneToOne(optional=true)
    @JoinColumn(name="mikvah_user_id")
    private MikvahUser mikvahUser;

    @Column(name="stripe_subscription_id")
    private String stripeSubscriptionId;

    @Column(name="start")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime start;

    @Column(name="expiration")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime expiration;

    @Enumerated(EnumType.STRING)
    private Plan plan;

    @Column(name="auto_renew_enabled")
    private boolean autoRenewEnabled;

}
