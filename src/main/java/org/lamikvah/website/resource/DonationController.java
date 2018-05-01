package org.lamikvah.website.resource;

import javax.servlet.http.HttpServletRequest;

import org.lamikvah.website.data.DonationRequest;
import org.lamikvah.website.data.MessageResponse;
import org.lamikvah.website.data.MikvahUser;
import org.lamikvah.website.service.DonationService;
import org.lamikvah.website.service.MikvahUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import lombok.extern.slf4j.Slf4j;

@RestController
@Slf4j
public class DonationController {

    @Autowired private DonationService donationService;
    @Autowired private MikvahUserService userService;


    @PostMapping("/donate")
    public MessageResponse donate(@RequestBody DonationRequest donationRequest, HttpServletRequest httpRequest) {

        MikvahUser user = null;
        try {
            user = userService.getUser(httpRequest);
        } catch (Exception e) {
            log.info("No user on donation request.");
        }

        donationService.donate(user, donationRequest.getName(), donationRequest.getEmail(), donationRequest.getAmount(), donationRequest.getToken());

        return MessageResponse.builder()
                .success(true)
                .message("Thank you for your donation!")
                .build();

    }

}
