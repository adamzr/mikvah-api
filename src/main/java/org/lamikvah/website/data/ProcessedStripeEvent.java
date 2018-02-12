package org.lamikvah.website.data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name="processed_stripe_event")
public class ProcessedStripeEvent {

    @Id
    @Column(name="stripe_event_id")
    private String stripeEventId;

}
