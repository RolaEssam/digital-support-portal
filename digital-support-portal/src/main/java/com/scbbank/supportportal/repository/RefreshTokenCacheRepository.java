package com.scbbank.supportportal.repository;

import com.scbbank.supportportal.dto.cache.RefreshTokenCache;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface RefreshTokenCacheRepository extends CrudRepository<RefreshTokenCache,String> {
    List<RefreshTokenCache> findByUserId(Long userId);
}
