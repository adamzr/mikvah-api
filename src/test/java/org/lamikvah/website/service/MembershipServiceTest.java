package org.lamikvah.website.service;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Optional;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.lamikvah.website.dao.MembershipRepository;
import org.lamikvah.website.dao.MikvahUserRepository;
import org.lamikvah.website.data.Membership;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.slf4j.Logger;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.ReflectionUtils;

public class MembershipServiceTest {

    private MembershipRepository membershipRepository;

    private MikvahUserRepository userRepository;

    private MembershipService membershipService;

    public MembershipServiceTest() {
        membershipService = new MembershipService();
        membershipRepository = mock(MembershipRepository.class);
        userRepository = mock(MikvahUserRepository.class);
        ReflectionTestUtils.setField(membershipService, "membershipRepository", membershipRepository);
    }

//    @Test
//    public void testCancelOfflineSubscriptions() throws Exception {
//        Membership membership = new Membership();
//        membership.setAutoRenewEnabled(false);
//        membership.setExpiration(LocalDateTime.of(2019, 1, 1, 1, 1));
//        when(membershipRepository.findAll()).thenReturn(Arrays.asList(membership));
//        membershipService.cancelOfflineSubscriptions();
//    }

}
