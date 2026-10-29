package dev.sentinel.persistence;

import dev.sentinel.engine.Decision;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "fraud_transaction")
public class TransactionEntity {

    @Id
    private UUID id;

    @Column(nullable = false, length = 64)
    private String externalId;

    @Column(nullable = false, unique = true, length = 100)
    private String idempotencyKey;

    @Column(nullable = false, length = 64)
    private String accountId;

    @Column(nullable = false, length = 64)
    private String merchantId;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(nullable = false, length = 2)
    private String country;

    @Column(length = 64)
    private String deviceId;

    @Column(nullable = false)
    private Instant occurredAt;

    @Column(nullable = false)
    private Instant receivedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 8)
    private Decision decision;

    @Column(nullable = false)
    private int riskScore;

    @Column(nullable = false, length = 64)
    private String requestHash;

    protected TransactionEntity() {}

    public TransactionEntity(UUID id, String externalId, String idempotencyKey, String accountId,
                             String merchantId, BigDecimal amount, String currency, String country,
                             String deviceId, Instant occurredAt, Instant receivedAt,
                             Decision decision, int riskScore, String requestHash) {
        this.id = id;
        this.externalId = externalId;
        this.idempotencyKey = idempotencyKey;
        this.accountId = accountId;
        this.merchantId = merchantId;
        this.amount = amount;
        this.currency = currency;
        this.country = country;
        this.deviceId = deviceId;
        this.occurredAt = occurredAt;
        this.receivedAt = receivedAt;
        this.decision = decision;
        this.riskScore = riskScore;
        this.requestHash = requestHash;
    }

    public UUID getId() { return id; }
    public String getExternalId() { return externalId; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public String getAccountId() { return accountId; }
    public String getMerchantId() { return merchantId; }
    public BigDecimal getAmount() { return amount; }
    public String getCurrency() { return currency; }
    public String getCountry() { return country; }
    public String getDeviceId() { return deviceId; }
    public Instant getOccurredAt() { return occurredAt; }
    public Instant getReceivedAt() { return receivedAt; }
    public Decision getDecision() { return decision; }
    public int getRiskScore() { return riskScore; }
    public String getRequestHash() { return requestHash; }
}
