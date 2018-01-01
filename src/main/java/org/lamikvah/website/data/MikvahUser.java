package org.lamikvah.website.data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Table;

import lombok.Data;

@Data
@Entity
@Table(indexes= {@Index(columnList="auth0_user_id", name="auth0_user_id_idx"), @Index(columnList="email", name="email_idx")})
public class MikvahUser {

    @Id
    @GeneratedValue(strategy=GenerationType.AUTO)
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
    
}
