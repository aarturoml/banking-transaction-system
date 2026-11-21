package com.example.banking.transfers.domain;

import com.example.banking.auth.domain.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;

@Entity
@Table(
        name = "idempotency_records",
        uniqueConstraints = @UniqueConstraint(
                name = "idempotency_records_unique_key",
                columnNames = {"actor_id", "operation", "idempotency_key"}
        )
)
public class IdempotencyRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "actor_id", nullable = false)
    private User actor;

    @Column(nullable = false, length = 100)
    private String operation;

    @Column(name = "idempotency_key", nullable = false, length = 255)
    private String key;

    @Column(name = "request_hash", nullable = false, length = 128)
    private String requestHash;

    @Column(name = "response_reference")
    private Long responseReference;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected IdempotencyRecord() {
    }

    public IdempotencyRecord(
            User actor,
            String operation,
            String key,
            String requestHash,
            Long responseReference
    ) {
        this.actor = actor;
        this.operation = operation;
        this.key = key;
        this.requestHash = requestHash;
        this.responseReference = responseReference;
        this.createdAt = Instant.now();
    }

    public String getRequestHash() {
        return requestHash;
    }

    public Long getResponseReference() {
        return responseReference;
    }
}
