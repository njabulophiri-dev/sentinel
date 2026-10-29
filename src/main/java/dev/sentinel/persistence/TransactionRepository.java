package dev.sentinel.persistence;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TransactionRepository extends JpaRepository<TransactionEntity, UUID> {

    Optional<TransactionEntity> findByIdempotencyKey(String idempotencyKey);

    List<TransactionEntity> findByAccountIdAndOccurredAtBetween(String accountId, Instant from, Instant to);

    @Query("select distinct t.deviceId from TransactionEntity t "
            + "where t.accountId = :accountId and t.deviceId is not null")
    Set<String> findKnownDevices(@Param("accountId") String accountId);
}
