package org.lamikvah.website.service;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import javax.servlet.http.HttpServletRequest;

import org.lamikvah.website.dao.AppointmentSlotRepository;
import org.lamikvah.website.dao.MikvahUserRepository;
import org.lamikvah.website.data.AppointmentSlot;
import org.lamikvah.website.data.AppointmentSlotDto;
import org.lamikvah.website.data.CreditCard;
import org.lamikvah.website.data.MikvahUser;
import org.lamikvah.website.data.UserDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import com.auth0.client.mgmt.ManagementAPI;
import com.auth0.client.mgmt.filter.UserFilter;
import com.auth0.exception.Auth0Exception;
import com.auth0.json.mgmt.users.User;
import com.auth0.net.Request;

@Component
public class MikvahUserService {
    
    @Autowired private MikvahUserRepository userRepository;
    @Autowired private CreditCardService creditCardService;
    @Autowired private AppointmentSlotRepository appointmentSlotRepository;

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
    
    public MikvahUser getUser(HttpServletRequest request) throws Auth0Exception {
        Principal principal = request.getUserPrincipal();
        String auth0UserId =  principal.getName();
        return getUser(auth0UserId);
    }
    
    public UserDto getUserWithCreditCardInfo(String auth0UserId) throws Auth0Exception {
        MikvahUser user = getUser(auth0UserId);
        return convertToUserDto(user);
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

    public void saveUser(MikvahUser user) {
        
        userRepository.save(user);
        
    }
    
    private UserDto convertToUserDto(MikvahUser user) {
        
        Optional<CreditCard> card = creditCardService.getCreditCard(user);
        CreditCard defaultCard = card.orElse(null);
        
        AppointmentSlotDto currentAppointment = null;
        List<AppointmentSlot> appointments = appointmentSlotRepository.findByStartBetweenAndMikvahUserOrderByStartAsc(LocalDateTime.now(), LocalDateTime.now().plusDays(30), user);
        if(!CollectionUtils.isEmpty(appointments)) {
            AppointmentSlot appointment = appointments.get(appointments.size() - 1);
            currentAppointment = AppointmentSlotDto.builder()
                    .id(appointment.getId())
                    .start(appointment.getStart())
                    .build();
        }
        
        return UserDto.builder()
                .auth0UserId(user.getAuth0UserId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .id(user.getId())
                .lastName(user.getLastName())
                .member(user.isMember())
                .stripeCustomerId(user.getStripeCustomerId())
                .title(user.getTitle())
                .defaultCard(defaultCard)
                .currentAppointment(currentAppointment)
                .build();
    }
}
