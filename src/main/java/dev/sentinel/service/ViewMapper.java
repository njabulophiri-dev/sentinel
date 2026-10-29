package dev.sentinel.service;

import dev.sentinel.api.ApiModels.AlertView;
import dev.sentinel.api.ApiModels.BlacklistView;
import dev.sentinel.api.ApiModels.ReasonView;
import dev.sentinel.api.ApiModels.TransactionView;
import dev.sentinel.persistence.AlertEntity;
import dev.sentinel.persistence.BlacklistEntity;
import dev.sentinel.persistence.RuleHitEntity;
import dev.sentinel.persistence.TransactionEntity;
import java.util.Comparator;
import java.util.List;

/** Turns database entities into the shapes the API returns. */
final class ViewMapper {

    private ViewMapper() {}

    static TransactionView transaction(TransactionEntity t, List<RuleHitEntity> hits) {
        List<ReasonView> reasons = hits.stream()
                .sorted(Comparator.comparingInt(RuleHitEntity::getScore).reversed()
                        .thenComparing(RuleHitEntity::getRuleId))
                .map(h -> new ReasonView(h.getRuleId(), h.getScore(), h.getReason()))
                .toList();
        return new TransactionView(t.getId(), t.getExternalId(), t.getAccountId(), t.getMerchantId(),
                t.getAmount(), t.getCurrency(), t.getCountry(), t.getDecision(), t.getRiskScore(),
                t.getOccurredAt(), t.getReceivedAt(), reasons);
    }

    static AlertView alert(AlertEntity a, TransactionView transaction) {
        return new AlertView(a.getId(), a.getStatus(), a.getCreatedAt(), a.getResolvedBy(),
                a.getResolvedAt(), a.getNote(), transaction);
    }

    static BlacklistView blacklist(BlacklistEntity b) {
        return new BlacklistView(b.getId(), b.getKind(), b.getValue(), b.getReason(),
                b.getCreatedBy(), b.getCreatedAt());
    }
}
