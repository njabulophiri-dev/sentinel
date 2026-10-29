package dev.sentinel.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AlertRepository extends JpaRepository<AlertEntity, Long> {

    Page<AlertEntity> findByStatus(AlertStatus status, Pageable pageable);
}
