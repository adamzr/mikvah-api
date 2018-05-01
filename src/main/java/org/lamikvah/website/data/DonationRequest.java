package org.lamikvah.website.data;

import lombok.Data;

@Data
public class DonationRequest {

    private double amount;

    private String token;

    private String name;

    private String email;

}
