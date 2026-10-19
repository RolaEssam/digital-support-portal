package com.scbbank.supportportal.repository;

import com.scbbank.supportportal.dto.cache.LoginAttemptCache;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

public interface LoginAttemptCacheRepository extends CrudRepository<LoginAttemptCache,String> {

}
