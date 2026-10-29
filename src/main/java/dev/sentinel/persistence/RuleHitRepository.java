package dev.sentinel.persistence;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RuleHitRepository extends JpaRepository<RuleHitEntity, Long> {

    List<RuleHitEntity> findByTransactionId(UUID transactionId);

    List<RuleHitEntity> findByTransactionIdIn(Collection<UUID> transactionIds);
}
