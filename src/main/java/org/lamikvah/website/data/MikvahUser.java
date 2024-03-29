package org.lamikvah.website.data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Table;

import lombok.Builder;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.base.MoreObjects;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(indexes= {
        @Index(columnList="auth0_user_id", name="mikvah_user_auth0_user_id_idx"),
        @Index(columnList="email", name="mikvah_user_email_idx"),
        @Index(columnList="stripe_customer_id", name="mikvah_user_stripe_customer_id_idx")
        })
public class MikvahUser {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long id;

    private String title;

    private String firstName;

    private String lastName;

    @Column(name="email")
    private String email;

    @Column(name="auth0_user_id")
    private String auth0UserId;

    @Column(name="stripe_customer_id")
    private String stripeCustomerId;

    private boolean member;

    private boolean admin;

    private String notes;

    private String phoneNumber;

    private String addressLine1;

    private String addressLine2;

    private String city;

    private String stateCode;

    private String postalCode;

    private String countryCode;

    @JsonIgnore
    public String getFullName() {
        String name = MoreObjects.firstNonNull(title, "") + " " + MoreObjects.firstNonNull(firstName, "") + " " + MoreObjects.firstNonNull(lastName, "");
        name = name.trim();
        if(StringUtils.isEmpty(name)) {
            return "Mikvah User";
        }
        return name;
    }

}
