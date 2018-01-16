package org.lamikvah.website.resource;

import java.security.Principal;

import javax.servlet.http.HttpServletRequest;

import org.lamikvah.website.data.MikvahUser;
import org.lamikvah.website.data.UserDto;
import org.lamikvah.website.data.UserRequestDto;
import org.lamikvah.website.exception.ServerErrorException;
import org.lamikvah.website.service.MikvahUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.auth0.exception.Auth0Exception;

import lombok.extern.slf4j.Slf4j;

@CrossOrigin
@RestController
@Slf4j
public class UserController {

    @Autowired private MikvahUserService service;

    @PostMapping("/user")
    public MikvahUser saveUser(HttpServletRequest request, @RequestBody UserRequestDto userRequest) {
        Principal principal = request.getUserPrincipal();
        String auth0UserId =  principal.getName();
        try {
            return service.saveUser(auth0UserId, userRequest);
        } catch (Auth0Exception e) {
            log.error("Failed to get email from Auth0.", e);
            throw new ServerErrorException("There was a problem saving your information. Please try again later.", e);
        }
    }

    @GetMapping("/user")
    public UserDto getUser(HttpServletRequest request, UserRequestDto userRequest) {

        Principal principal = request.getUserPrincipal();
        String auth0UserId = principal.getName();
        try {
            return service.getUserWithCreditCardInfo(auth0UserId);
        } catch (Auth0Exception e) {
            log.error("Failed to get email from Auth0.", e);
            throw new ServerErrorException("There was a problem getting your user information. Please try again later.", e);
        }

    }
}
