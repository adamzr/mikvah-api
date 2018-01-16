package org.lamikvah.website.dao;

import java.util.Optional;

import org.lamikvah.website.data.Membership;
import org.lamikvah.website.data.MikvahUser;
import org.springframework.data.repository.CrudRepository;

public interface MembershipRepository extends CrudRepository<Membership, Long>{

    Optional<Membership> findByMikvahUser(MikvahUser mikvahUser);

    Optional<Membership> findByStripeSubscriptionId(String stripeSubscriptionId);

}
