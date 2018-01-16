package org.lamikvah.website.resource;

import javax.servlet.http.HttpServletRequest;

import org.lamikvah.website.data.MikvahUser;
import org.lamikvah.website.data.TokenRequest;
import org.lamikvah.website.service.CreditCardService;
import org.lamikvah.website.service.MikvahUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;


@CrossOrigin
@RestController
public class CreditCardController {

    @Autowired private CreditCardService creditCardService;
    @Autowired private MikvahUserService mikvahUserService;

    @PostMapping("/credit-card")
    public String addToken(HttpServletRequest request, @RequestBody TokenRequest tokenRequest){
        MikvahUser user = mikvahUserService.getUser(request);
        creditCardService.addNewCreditCard(user, tokenRequest.getToken());
        return "\"Saved card for token " + tokenRequest.getToken() + "\"";
    }

}
