package org.lamikvah.website.resource;

import java.security.Principal;

import javax.servlet.http.HttpServletRequest;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@CrossOrigin
@RestController
public class AppointmentController {

    @GetMapping(value = "/test-auth")
    public String testAuth(HttpServletRequest request) {
        Principal principal = request.getUserPrincipal();
        return principal.getName();
    }

    @GetMapping(value = "/test-no-auth")
    public String testNoAuth() {
        return "Hello, World!";
    }

}
