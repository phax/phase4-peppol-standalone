package com.mysupply.phase4.persistence;

import com.mysupply.phase4.models.ApiKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface ApiKeyRepository extends JpaRepository<ApiKey, Long> {
    Optional<ApiKey> findByKeyName(String keyName);
    Optional<ApiKey> findByKeyValue(String keyValue);
}
