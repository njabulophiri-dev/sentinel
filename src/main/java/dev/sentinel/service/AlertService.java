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
    public PageView<AlertView> list(AlertStatus status, int page, int size) {
        var pageRequest = PageParams.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<AlertEntity> found = status == null ? alerts.findAll(pageRequest) : alerts.findByStatus(status, pageRequest);

        List<UUID> txIds = found.getContent().stream().map(AlertEntity::getTransactionId).toList();
        Map<UUID, TransactionEntity> txById = txIds.isEmpty() ? Map.of()
                : transactions.findAllById(txIds).stream()
                        .collect(Collectors.toMap(TransactionEntity::getId, Function.identity()));
        Map<UUID, List<RuleHitEntity>> hitsById = txIds.isEmpty() ? Map.of()
                : ruleHits.findByTransactionIdIn(txIds).stream()
                        .collect(Collectors.groupingBy(RuleHitEntity::getTransactionId));

        List<AlertView> items = found.getContent().stream()
                .map(a -> ViewMapper.alert(a, ViewMapper.transaction(
                        txById.get(a.getTransactionId()), hitsById.getOrDefault(a.getTransactionId(), List.of()))))
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
