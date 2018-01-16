package org.lamikvah.website.dao;

import org.lamikvah.website.data.ProcessedStripeEvent;
import org.springframework.data.repository.CrudRepository;

public interface ProcessedStripeEventRespository extends CrudRepository<ProcessedStripeEvent, String> {

}
