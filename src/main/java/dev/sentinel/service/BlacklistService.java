package dev.sentinel.service;

import dev.sentinel.api.ApiModels.BlacklistRequest;
import dev.sentinel.api.ApiModels.BlacklistView;
import dev.sentinel.persistence.BlacklistEntity;
import dev.sentinel.persistence.BlacklistRepository;
import java.time.Clock;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BlacklistService {

    private final BlacklistRepository blacklist;
    private final AuditService audit;
    private final Clock clock;

    public BlacklistService(BlacklistRepository blacklist, AuditService audit, Clock clock) {
        this.blacklist = blacklist;
        this.audit = audit;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<BlacklistView> list() {
        return blacklist.findAllByOrderByCreatedAtDesc().stream().map(ViewMapper::blacklist).toList();
    }

    @Transactional
    public BlacklistView add(BlacklistRequest request, String actor) {
        String value = request.value().trim();
        if (blacklist.existsByKindAndValue(request.kind(), value)) {
            throw new ApiErrors.Conflict(request.kind() + " " + value + " is already blacklisted");
        }
        BlacklistEntity saved = blacklist.saveAndFlush(
                new BlacklistEntity(request.kind(), value, request.reason(), actor, clock.instant()));
        audit.record(actor, "BLACKLIST_ADDED", request.kind() + ":" + value, request.reason());
        return ViewMapper.blacklist(saved);
    }

    @Transactional
    public void remove(long id, String actor) {
        BlacklistEntity entry = blacklist.findById(id)
                .orElseThrow(() -> new ApiErrors.NotFound("Blacklist entry not found"));
        blacklist.delete(entry);
        audit.record(actor, "BLACKLIST_REMOVED", entry.getKind() + ":" + entry.getValue(), null);
    }
}
