package dev.sentinel.service;

import dev.sentinel.api.ApiModels.IngestRequest;
import dev.sentinel.api.ApiModels.TransactionView;
import dev.sentinel.engine.Assessment;
import dev.sentinel.engine.BlacklistKind;
import dev.sentinel.engine.Decision;
import dev.sentinel.engine.FraudEngine;
import dev.sentinel.engine.RiskContext;
import dev.sentinel.engine.TxFacts;
import dev.sentinel.persistence.AlertEntity;
import dev.sentinel.persistence.AlertRepository;
import dev.sentinel.persistence.BlacklistRepository;
import dev.sentinel.persistence.RuleHitEntity;
import dev.sentinel.persistence.RuleHitRepository;
import dev.sentinel.persistence.TransactionEntity;
import dev.sentinel.persistence.TransactionRepository;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Assesses a transaction and stores the result, exactly once per Idempotency-Key.
 *
 * <p>Two requests with the same key can arrive at the same moment. Both pass the "have I seen this
 * key?" check, and the database's UNIQUE constraint lets only one insert win. The loser catches the
 * constraint violation and returns the winner's stored result, so the caller sees one transaction.
 */
@Service
public class IngestionService {

    private static final Duration MAX_FUTURE_SKEW = Duration.ofMinutes(5);

    /** The stored transaction, and whether it was a replay of an earlier request. */
    public record Result(TransactionView transaction, boolean replayed) {}

    private final TransactionRepository transactions;
    private final RuleHitRepository ruleHits;
    private final AlertRepository alerts;
    private final BlacklistRepository blacklist;
    private final FraudEngine engine;
    private final Clock clock;
    private final MeterRegistry meters;
    private final TransactionTemplate writeTx;
    private final TransactionTemplate readTx;

    public IngestionService(TransactionRepository transactions, RuleHitRepository ruleHits,
                            AlertRepository alerts, BlacklistRepository blacklist, FraudEngine engine,
                            Clock clock, MeterRegistry meters, PlatformTransactionManager txManager) {
        this.transactions = transactions;
        this.ruleHits = ruleHits;
        this.alerts = alerts;
        this.blacklist = blacklist;
        this.engine = engine;
        this.clock = clock;
        this.meters = meters;
        this.writeTx = new TransactionTemplate(txManager);
        this.readTx = new TransactionTemplate(txManager);
        this.readTx.setReadOnly(true);
    }

    public Result ingest(String idempotencyKey, IngestRequest request) {
        String hash = RequestHasher.hash(
                request.externalId(), request.accountId(), request.merchantId(),
                request.amount().setScale(2).toPlainString(), request.currency(), request.country(),
                request.deviceId(), request.occurredAt().toString());
        try {
            Result result = writeTx.execute(status -> create(idempotencyKey, request, hash));
            if (!result.replayed()) {
                meters.counter("sentinel.decisions", "decision", result.transaction().decision().name())
                        .increment();
            }
            return result;
        } catch (DataIntegrityViolationException e) {
            // Lost a race with an identical request: return what the winner stored.
            Optional<Result> existing = readTx.execute(status -> findReplay(idempotencyKey, hash));
            return existing.orElseThrow(() -> e);
        }
    }

    private Result create(String key, IngestRequest request, String hash) {
        Optional<Result> replay = findReplay(key, hash);
        if (replay.isPresent()) {
            return replay.get();
        }
        Instant now = clock.instant();
        if (request.occurredAt().isAfter(now.plus(MAX_FUTURE_SKEW))) {
            throw new ApiErrors.BadRequest("occurredAt is too far in the future");
        }

        UUID id = UUID.randomUUID();
        TxFacts facts = new TxFacts(id.toString(), request.accountId(), request.merchantId(), request.amount(),
                request.currency(), request.country(), request.deviceId(), request.occurredAt());

        // One query loads all the history any rule needs; the engine itself never touches the database.
        Instant from = request.occurredAt().minus(engine.lookback());
        List<TxFacts> recent = transactions
                .findByAccountIdAndOccurredAtBetween(request.accountId(), from, request.occurredAt())
                .stream().map(IngestionService::toFacts).toList();
        Set<String> knownDevices = transactions.findKnownDevices(request.accountId());

        Assessment assessment = engine.assess(
                new RiskContext(facts, recent, knownDevices, blacklistMatches(request)));

        TransactionEntity entity = new TransactionEntity(id, request.externalId(), key, request.accountId(),
                request.merchantId(), request.amount(), request.currency(), request.country(),
                request.deviceId(), request.occurredAt(), now, assessment.decision(), assessment.score(), hash);
        transactions.saveAndFlush(entity); // flush now so a duplicate key fails inside this transaction

        List<RuleHitEntity> hits = assessment.hits().stream()
                .map(h -> new RuleHitEntity(id, h.ruleId(), h.score(), h.reason()))
                .toList();
        ruleHits.saveAll(hits);

        if (assessment.decision() != Decision.APPROVE) {
            alerts.save(new AlertEntity(id, now));
        }
        return new Result(ViewMapper.transaction(entity, hits), false);
    }

    private Optional<Result> findReplay(String key, String hash) {
        return transactions.findByIdempotencyKey(key).map(existing -> {
            if (!existing.getRequestHash().equals(hash)) {
                throw new ApiErrors.Conflict("This Idempotency-Key was already used with a different request");
            }
            return new Result(
                    ViewMapper.transaction(existing, ruleHits.findByTransactionId(existing.getId())), true);
        });
    }

    private Set<BlacklistKind> blacklistMatches(IngestRequest request) {
        Set<BlacklistKind> matches = EnumSet.noneOf(BlacklistKind.class);
        if (blacklist.existsByKindAndValue(BlacklistKind.ACCOUNT, request.accountId())) {
            matches.add(BlacklistKind.ACCOUNT);
        }
        if (blacklist.existsByKindAndValue(BlacklistKind.MERCHANT, request.merchantId())) {
            matches.add(BlacklistKind.MERCHANT);
        }
        if (request.deviceId() != null
                && blacklist.existsByKindAndValue(BlacklistKind.DEVICE, request.deviceId())) {
            matches.add(BlacklistKind.DEVICE);
        }
        return matches;
    }

    private static TxFacts toFacts(TransactionEntity t) {
        return new TxFacts(t.getId().toString(), t.getAccountId(), t.getMerchantId(), t.getAmount(),
                t.getCurrency(), t.getCountry(), t.getDeviceId(), t.getOccurredAt());
    }
}
