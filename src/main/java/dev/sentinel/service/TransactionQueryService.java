package dev.sentinel.service;

import dev.sentinel.api.ApiModels.PageView;
import dev.sentinel.api.ApiModels.TransactionView;
import dev.sentinel.persistence.RuleHitEntity;
import dev.sentinel.persistence.RuleHitRepository;
import dev.sentinel.persistence.TransactionEntity;
import dev.sentinel.persistence.TransactionRepository;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class TransactionQueryService {

    private final TransactionRepository transactions;
    private final RuleHitRepository ruleHits;

    public TransactionQueryService(TransactionRepository transactions, RuleHitRepository ruleHits) {
        this.transactions = transactions;
        this.ruleHits = ruleHits;
    }

    public TransactionView get(UUID id) {
        TransactionEntity t = transactions.findById(id)
                .orElseThrow(() -> new ApiErrors.NotFound("Transaction not found"));
        return ViewMapper.transaction(t, ruleHits.findByTransactionId(id));
    }

    public PageView<TransactionView> list(int page, int size) {
        Page<TransactionEntity> found = transactions.findAll(
                PageParams.of(page, size, Sort.by(Sort.Direction.DESC, "receivedAt")));
        List<UUID> ids = found.getContent().stream().map(TransactionEntity::getId).toList();
        // One query for every row's reasons, instead of one query per row.
        Map<UUID, List<RuleHitEntity>> hitsById = ids.isEmpty() ? Map.of()
                : ruleHits.findByTransactionIdIn(ids).stream()
                        .collect(Collectors.groupingBy(RuleHitEntity::getTransactionId));
        List<TransactionView> items = found.getContent().stream()
                .map(t -> ViewMapper.transaction(t, hitsById.getOrDefault(t.getId(), List.of())))
                .toList();
        return new PageView<>(items, found.getNumber(), found.getSize(), found.getTotalElements(),
                found.getTotalPages());
    }
}
