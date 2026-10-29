package dev.sentinel.service;

import dev.sentinel.persistence.AuditEntity;
import dev.sentinel.persistence.AuditRepository;
import java.time.Clock;
import org.springframework.stereotype.Service;

/** Writes to the append-only audit log. */
@Service
public class AuditService {

    private final AuditRepository audit;
    private final Clock clock;

    public AuditService(AuditRepository audit, Clock clock) {
        this.audit = audit;
        this.clock = clock;
    }

    public void record(String actor, String action, String subject, String detail) {
        audit.save(new AuditEntity(clock.instant(), abbreviate(actor, 64), action,
                abbreviate(subject, 100), abbreviate(detail, 500)));
    }

    static String abbreviate(String value, int max) {
        if (value == null) {
            return null;
        }
        return value.length() <= max ? value : value.substring(0, max);
    }
}
