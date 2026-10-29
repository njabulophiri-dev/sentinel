package dev.sentinel.persistence;

import dev.sentinel.engine.BlacklistKind;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BlacklistRepository extends JpaRepository<BlacklistEntity, Long> {

    boolean existsByKindAndValue(BlacklistKind kind, String value);

    List<BlacklistEntity> findAllByOrderByCreatedAtDesc();
}
