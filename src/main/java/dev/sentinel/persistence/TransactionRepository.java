package dev.sentinel.persistence;

import java.time.Instant;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TransactionRepository extends JpaRepository<TransactionEntity, UUID> {

    Optional<TransactionEntity> findByIdempotencyKey(String idempotencyKey);

    long countByIdempotencyKey(String idempotencyKey);

    long countByAccountIdAndOccurredAtBetween(String accountId, Instant from, Instant to);

    @Query("select count(t) from TransactionEntity t where t.accountId = :accountId "
            + "and t.occurredAt between :from and :to and t.amount <= :maxAmount")
    long countSmallTransactions(@Param("accountId") String accountId, @Param("from") Instant from,
                                @Param("to") Instant to, @Param("maxAmount") BigDecimal maxAmount);

    Optional<TransactionEntity> findFirstByAccountIdAndCountryNotAndOccurredAtBetweenOrderByOccurredAtDesc(
            String accountId, String country, Instant from, Instant to);

    boolean existsByAccountIdAndDeviceIdIsNotNull(String accountId);

    boolean existsByAccountIdAndDeviceId(String accountId, String deviceId);
}
