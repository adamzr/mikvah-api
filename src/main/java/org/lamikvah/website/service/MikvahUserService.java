package org.lamikvah.website.service;

import java.util.Optional;

import org.lamikvah.website.dao.MikvahUserRepository;
import org.lamikvah.website.data.MikvahUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.auth0.client.mgmt.ManagementAPI;
import com.auth0.client.mgmt.filter.UserFilter;
import com.auth0.exception.Auth0Exception;
import com.auth0.json.mgmt.users.User;
import com.auth0.net.Request;

@Component
public class MikvahUserService {
    
    @Autowired private MikvahUserRepository userRepository;

    private ManagementAPI auth0ManagementApi = new ManagementAPI("{YOUR_DOMAIN}", "{YOUR_API_TOKEN}");

    private static final UserFilter NO_OP_USER_FILTER = new UserFilter();
    
    @Autowired
    public MikvahUserService(@Value("${auth0.issuer}") String domain, @Value("${AUTH0_MANAGEMENT_TOKEN}") String token) {
        auth0ManagementApi = new ManagementAPI(domain, token);
    }
    
    private String getUserEmail(String principalName) throws Auth0Exception {
        
        Request<User> apiRequest = auth0ManagementApi.users().get(principalName, NO_OP_USER_FILTER);
        User user = apiRequest.execute();
        return user.getEmail();
        
    }
    
    public MikvahUser saveUser(String auth0UserId, String title, String firstName, String lastName) throws Auth0Exception {
        
        MikvahUser user = userRepository.getByAuth0UserId(auth0UserId).orElse(new MikvahUser());
        if(user.getEmail() == null) {
            String email = getUserEmail(auth0UserId);
            user.setEmail(email);
        }
        user.setTitle(title);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setAuth0UserId(auth0UserId);
        return userRepository.save(user);
        
    }
    
    public MikvahUser getUser(String auth0UserId) throws Auth0Exception {
        
        Optional<MikvahUser> user = userRepository.getByAuth0UserId(auth0UserId);
        if(!user.isPresent()) {
            MikvahUser newUser = new MikvahUser();
            newUser.setAuth0UserId(auth0UserId);
            String email = getUserEmail(auth0UserId);
            newUser.setEmail(email);
            return userRepository.save(newUser);
        }
        return user.get();
        
    }
}
