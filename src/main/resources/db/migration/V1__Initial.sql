CREATE TABLE mikvah_user (
    id int8 NOT NULL,
    auth0_user_id varchar(255) NULL,
    email varchar(255) NULL,
    first_name varchar(255) NULL,
    last_name varchar(255) NULL,
    title varchar(255) NULL,
    "member" bool NOT NULL,
    stripe_customer_id varchar NULL,
    address_line1 varchar(255) NULL,
    address_line2 varchar(255) NULL,
    city varchar(255) NULL,
    country_code varchar(255) NULL,
    notes varchar(255) NULL,
    phone_number varchar(255) NULL,
    postal_code varchar(255) NULL,
    state_code varchar(255) NULL,
    PRIMARY KEY (id)
)
WITH (
    OIDS=FALSE
) ;
CREATE INDEX mikvah_user_auth0_user_id_idx ON mikvah_user USING btree (auth0_user_id) ;
CREATE INDEX mikvah_user_email_idx ON mikvah_user USING btree (email) ;
CREATE INDEX mikvah_userstripe_customer_id_idx ON mikvah_user USING btree (stripe_customer_id) ;

CREATE TABLE appointment_slot (
    id int8 NOT NULL,
    "start" timestamp NULL,
    mikvah_user_id int8 NULL,
    stripe_charge_id varchar NULL,
    notes varchar(255) NULL,
    PRIMARY KEY (id),
    FOREIGN KEY (mikvah_user_id) REFERENCES mikvah_user(id)
)
WITH (
    OIDS=FALSE
) ;
CREATE INDEX appointment_slot_mikvah_user_id_idx ON appointment_slot USING btree (mikvah_user_id) ;
CREATE INDEX appointment_slot_start_idx ON appointment_slot USING btree (start) ;

CREATE TABLE daily_hours (
    "day" date NOT NULL,
    closed bool NOT NULL,
    closing time NULL,
    opening time NULL,
    PRIMARY KEY (day)
)
WITH (
    OIDS=FALSE
) ;
CREATE INDEX daily_hours_day_idx ON daily_hours USING btree (day) ;

CREATE TABLE membership (
    id int8 NOT NULL,
    expiration timestamp NULL,
    plan varchar(255) NULL,
    "start" timestamp NULL,
    stripe_subscription_id varchar(255) NULL,
    mikvah_user_id int8 NULL,
    PRIMARY KEY (id),
    FOREIGN KEY (mikvah_user_id) REFERENCES mikvah_user(id)
)
WITH (
    OIDS=FALSE
) ;
CREATE INDEX membership_expiration_idx ON membership USING btree (expiration) ;
CREATE INDEX membership_mikvah_user_id_idx ON membership USING btree (mikvah_user_id) ;
CREATE INDEX membership_stripe_subscription_id_idx ON membership USING btree (stripe_subscription_id) ;

CREATE TABLE reservation_history_log (
    id int8 NOT NULL,
    "action" varchar(255) NULL,
    created timestamp NULL,
    stripe_id varchar(255) NULL,
    appointment_slot_id int8 NULL,
    mikvah_user_id int8 NULL,
    PRIMARY KEY (id),
    FOREIGN KEY (appointment_slot_id) REFERENCES appointment_slot(id),
    FOREIGN KEY (mikvah_user_id) REFERENCES mikvah_user(id)
)
WITH (
    OIDS=FALSE
) ;
CREATE INDEX rhl_created_idx ON reservation_history_log USING btree (created) ;
CREATE INDEX rhl_mikvah_user_id_idx ON reservation_history_log USING btree (mikvah_user_id) ;
CREATE INDEX rhl_stripe_id_idx ON reservation_history_log USING btree (stripe_id) ;

CREATE TABLE processed_stripe_event (
    stripe_event_id varchar(255) NOT NULL,
    PRIMARY KEY(stripe_event_id)
)
WITH (
    OIDS=FALSE
) ;