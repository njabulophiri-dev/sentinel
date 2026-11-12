package dev.sentinel.service;

import dev.sentinel.api.ApiModels.AlertView;
import dev.sentinel.api.ApiModels.PageView;
import dev.sentinel.api.ApiModels.ResolveRequest;
import dev.sentinel.persistence.AlertEntity;
import dev.sentinel.persistence.AlertRepository;
import dev.sentinel.persistence.AlertStatus;
import dev.sentinel.persistence.RuleHitEntity;
import dev.sentinel.persistence.RuleHitRepository;
import dev.sentinel.persistence.TransactionEntity;
import dev.sentinel.persistence.TransactionRepository;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;
import org.springframework.data.core.TypedPropertyPath;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AlertService {

    private final AlertRepository alerts;
    private final TransactionRepository transactions;
    private final RuleHitRepository ruleHits;
    private final AuditService audit;
    private final Clock clock;

    public AlertService(AlertRepository alerts, TransactionRepository transactions, RuleHitRepository ruleHits,
                        AuditService audit, Clock clock) {
        this.alerts = alerts;
        this.transactions = transactions;
        this.ruleHits = ruleHits;
        this.audit = audit;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public PageView<AlertView> list(@Nullable AlertStatus status, int page, int size) {
        var pageRequest = PageParams.of(page, size,
                Sort.by(Sort.Direction.DESC,
                        TypedPropertyPath.path((AlertEntity alert) -> alert.getCreatedAt())));
        Page<AlertEntity> found = status == null ? alerts.findAll(pageRequest) : alerts.findByStatus(status, pageRequest);

        List<UUID> txIds = found.getContent().stream().map(alert -> alert.getTransactionId()).toList();
        Map<UUID, TransactionEntity> txById = txIds.isEmpty() ? Map.of()
                : transactions.findAllById(txIds).stream()
                        .collect(Collectors.toMap(transaction -> transaction.getId(), Function.identity()));
        Map<UUID, List<RuleHitEntity>> hitsById = txIds.isEmpty() ? Map.of()
                : ruleHits.findByTransactionIdIn(txIds).stream()
                        .collect(Collectors.groupingBy(hit -> hit.getTransactionId()));

        List<AlertView> items = found.getContent().stream()
                .map(a -> {
                    UUID txId = a.getTransactionId();
                    TransactionEntity tx = txById.get(txId);
                    if (tx == null) {
                        throw new IllegalStateException("Alert " + a.getId() + " has no transaction");
                    }
                    return ViewMapper.alert(a,
                            ViewMapper.transaction(tx, hitsById.getOrDefault(txId, List.of())));
                })
                .toList();
        return new PageView<>(items, found.getNumber(), found.getSize(), found.getTotalElements(),
                found.getTotalPages());
    }

    @Transactional(readOnly = true)
    public AlertView get(long id) {
        return view(load(id));
    }

    @Transactional
    public AlertView resolve(long id, ResolveRequest request, String actor) {
        if (request.status() == AlertStatus.OPEN) {
            throw new ApiErrors.BadRequest("status must be CONFIRMED_FRAUD or FALSE_POSITIVE");
        }
        AlertEntity alert = load(id);
        if (alert.getStatus() != AlertStatus.OPEN) {
            throw new ApiErrors.Conflict("Alert was already resolved as " + alert.getStatus());
        }
        alert.resolve(request.status(), actor, request.note(), clock.instant());
        AlertEntity saved = alerts.saveAndFlush(alert);
        audit.record(actor, "ALERT_RESOLVED", "alert:" + id, request.status().name());
        return view(saved);
    }

    private AlertEntity load(long id) {
        return alerts.findById(id).orElseThrow(() -> new ApiErrors.NotFound("Alert not found"));
    }

    private AlertView view(AlertEntity alert) {
        TransactionEntity tx = transactions.findById(alert.getTransactionId())
                .orElseThrow(() -> new IllegalStateException("Alert " + alert.getId() + " has no transaction"));
        return ViewMapper.alert(alert, ViewMapper.transaction(tx, ruleHits.findByTransactionId(tx.getId())));
    }
}
