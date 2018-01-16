package org.lamikvah.website.dao;

import java.util.List;

import org.lamikvah.website.data.MikvahUser;
import org.lamikvah.website.data.ReservationHistoryLog;
import org.springframework.data.repository.CrudRepository;

public interface ReservationHistoryLogRepository extends CrudRepository<ReservationHistoryLog, Long> {

    List<ReservationHistoryLog> findByMikvahUserOrderByCreatedDesc(MikvahUser user);
    
}
