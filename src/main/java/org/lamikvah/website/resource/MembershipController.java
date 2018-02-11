package org.lamikvah.website.resource;

import javax.servlet.http.HttpServletRequest;

import org.lamikvah.website.data.AutoRenewRequest;
import org.lamikvah.website.data.MembershipRequest;
import org.lamikvah.website.data.MessageResponse;
import org.lamikvah.website.data.MikvahUser;
import org.lamikvah.website.data.Plan;
import org.lamikvah.website.service.MembershipService;
import org.lamikvah.website.service.MikvahUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.stripe.exception.APIConnectionException;
import com.stripe.exception.APIException;
import com.stripe.exception.AuthenticationException;
import com.stripe.exception.CardException;
import com.stripe.exception.InvalidRequestException;

import lombok.extern.slf4j.Slf4j;

@RestController
@Slf4j
public class MembershipController {

    @Autowired private MembershipService service;
    @Autowired private MikvahUserService userService;


    @PostMapping("/membership")
    public MessageResponse signUpForMembership(@RequestBody MembershipRequest membershipRequest, HttpServletRequest httpRequest) {

        MikvahUser user = userService.getUser(httpRequest);

        service.createMembership(user, Plan.forStripePlanName(membershipRequest.getPlan()).get());

        return MessageResponse.builder()
                .success(true)
                .message("Thank you for joining the mikvah! Your membership helps support our communty's mikavh. You will receive an email when your membership is activated.")
                .build();
    }

    @PostMapping("/auto-renew")
    public MessageResponse modifyAutoRenew(@RequestBody AutoRenewRequest autoRenewRequest, HttpServletRequest httpRequest) {

        MikvahUser user = userService.getUser(httpRequest);

        if(autoRenewRequest.isEnabled()) {
            try {
                service.enableAutoRenew(user);
            } catch (AuthenticationException | InvalidRequestException | APIConnectionException | CardException
                    | APIException e) {
                log.error("Error enabling auto-renew for user={}.", user, e);
                return MessageResponse.builder().message("Sorry, there was a problem enabling auto-renew. Please try again later.").success(false).build();
            }
            return MessageResponse.builder().message("Auto-renew was enabled for your subscription. Thank you!").success(true).build();

        } else {
            try {
                service.disableAutoRenew(user);
            } catch (AuthenticationException | InvalidRequestException | APIConnectionException | CardException
                    | APIException e) {
                log.error("Error disabling auto-renew for user={}.", user, e);
                return MessageResponse.builder().message("Sorry, there was a problem disabling auto-renew. Please try again later.").success(false).build();
            }
            return MessageResponse.builder().message("Auto-renew was disabled for your subscription.").success(true).build();

        }
    }

}
