package org.lamikvah.website.dao;

import java.util.Optional;

import org.lamikvah.website.data.MikvahUser;
import org.springframework.data.repository.CrudRepository;

public interface MikvahUserRepository extends CrudRepository<MikvahUser, Long>{

    public Optional<MikvahUser> getByAuth0UserId(String auth0UserId);

    public Optional<MikvahUser> findByStripeCustomerId(String stripeCustomerId);

}
