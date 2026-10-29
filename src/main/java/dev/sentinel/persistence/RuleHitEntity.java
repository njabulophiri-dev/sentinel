package dev.sentinel.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "rule_hit")
public class RuleHitEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private UUID transactionId;

    @Column(nullable = false, length = 48)
    private String ruleId;

    @Column(nullable = false)
    private int score;

    @Column(nullable = false, length = 300)
    private String reason;

    protected RuleHitEntity() {}

    public RuleHitEntity(UUID transactionId, String ruleId, int score, String reason) {
        this.transactionId = transactionId;
        this.ruleId = ruleId;
        this.score = score;
        this.reason = reason;
    }

    public Long getId() { return id; }
    public UUID getTransactionId() { return transactionId; }
    public String getRuleId() { return ruleId; }
    public int getScore() { return score; }
    public String getReason() { return reason; }
}
