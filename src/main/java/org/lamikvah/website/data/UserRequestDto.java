package org.lamikvah.website.data;

import lombok.Data;

@Data
public class UserRequestDto {

    private String title;
    private String firstName;
    private String lastName;
    private String notes;
    private String phoneNumber;
    private String addressLine1;
    private String addressLine2;
    private String city;
    private String stateCode;
    private String postalCode;
    private String countryCode;

}
