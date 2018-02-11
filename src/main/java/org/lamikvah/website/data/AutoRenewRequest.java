package org.lamikvah.website.data;

import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level=AccessLevel.PRIVATE)
public class AutoRenewRequest {

    boolean enabled;

}
