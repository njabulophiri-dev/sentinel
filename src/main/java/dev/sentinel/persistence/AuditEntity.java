package dev.sentinel.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "audit_log")
public class AuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Instant occurredAt;

    @Column(nullable = false, length = 64)
    private String actor;

    @Column(nullable = false, length = 48)
    private String action;

    @Column(nullable = false, length = 100)
    private String subject;

    @Column(length = 500)
    private String detail;

    protected AuditEntity() {}

    public AuditEntity(Instant occurredAt, String actor, String action, String subject, String detail) {
        this.occurredAt = occurredAt;
        this.actor = actor;
        this.action = action;
        this.subject = subject;
        this.detail = detail;
    }

    public Long getId() { return id; }
    public Instant getOccurredAt() { return occurredAt; }
    public String getActor() { return actor; }
    public String getAction() { return action; }
    public String getSubject() { return subject; }
    public String getDetail() { return detail; }
}
