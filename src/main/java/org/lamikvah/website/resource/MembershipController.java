package org.lamikvah.website.resource;

import javax.servlet.http.HttpServletRequest;

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

@RestController
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
}
