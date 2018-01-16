package org.lamikvah.website.data;

import lombok.Builder;
import lombok.Value;

@Builder
@Value
public class MessageResponse {

    boolean success;
    String message;

}
