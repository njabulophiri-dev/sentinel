package dev.sentinel.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "alert")
public class AlertEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private UUID transactionId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AlertStatus status;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(length = 64)
    private String resolvedBy;

    private Instant resolvedAt;

    @Column(length = 500)
    private String note;

    /** Optimistic lock: if two analysts resolve the same alert, the second one gets a conflict. */
    @Version
    private Long version;

    protected AlertEntity() {}

    public AlertEntity(UUID transactionId, Instant createdAt) {
        this.transactionId = transactionId;
        this.createdAt = createdAt;
        this.status = AlertStatus.OPEN;
    }

    public void resolve(AlertStatus outcome, String actor, String note, Instant at) {
        this.status = outcome;
        this.resolvedBy = actor;
        this.note = note;
        this.resolvedAt = at;
    }

    public Long getId() { return id; }
    public UUID getTransactionId() { return transactionId; }
    public AlertStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public String getResolvedBy() { return resolvedBy; }
    public Instant getResolvedAt() { return resolvedAt; }
    public String getNote() { return note; }
}
