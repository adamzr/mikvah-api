package org.lamikvah.website.service;

import org.springframework.stereotype.Component;

import com.auth0.client.mgmt.ManagementAPI;
import com.auth0.client.mgmt.filter.UserFilter;
import com.auth0.json.mgmt.users.User;
import com.auth0.net.Request;

@Component
public class UserService {

    private ManagementAPI auth0ManagementApi = new ManagementAPI("{YOUR_DOMAIN}", "{YOUR_API_TOKEN}");

    private static final UserFilter NO_OP_USER_FILTER = new UserFilter();
    
    public String getUserEmail(String principalName) {
        
        Request<User> apiRequest = auth0ManagementApi.users().get(principalName, NO_OP_USER_FILTER);
        User user = apiRequest.execute();
        return user.getEmail();
        
    }
}
