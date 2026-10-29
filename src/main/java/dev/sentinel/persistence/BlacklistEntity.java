package dev.sentinel.persistence;

import dev.sentinel.engine.BlacklistKind;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "blacklist_entry")
public class BlacklistEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private BlacklistKind kind;

    @Column(name = "entry_value", nullable = false, length = 64)
    private String value;

    @Column(length = 200)
    private String reason;

    @Column(nullable = false, length = 64)
    private String createdBy;

    @Column(nullable = false)
    private Instant createdAt;

    protected BlacklistEntity() {}

    public BlacklistEntity(BlacklistKind kind, String value, String reason, String createdBy, Instant createdAt) {
        this.kind = kind;
        this.value = value;
        this.reason = reason;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public BlacklistKind getKind() { return kind; }
    public String getValue() { return value; }
    public String getReason() { return reason; }
    public String getCreatedBy() { return createdBy; }
    public Instant getCreatedAt() { return createdAt; }
}
