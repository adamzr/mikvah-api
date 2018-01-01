package org.lamikvah.website.data;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CreditCard {

    private String brand;
    private String last4;
    private String tokenId;
    private int expirationMonth;
    private int expirationYear;
    
}
