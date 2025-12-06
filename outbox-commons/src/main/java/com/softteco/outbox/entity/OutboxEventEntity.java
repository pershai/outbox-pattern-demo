package com.softteco.outbox.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

/**
 * Standard outbox event entity for the transactional outbox pattern.
 * This entity ensures that domain events are reliably published to message
 * brokers
 * by storing them in the same database transaction as the business operation.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "outbox_events")
public class OutboxEventEntity {

    /**
     * Unique identifier for the event, also serves as the primary key.
     */
    @Id
    @Column(name = "event_id")
    private UUID eventId;

    /**
     * Type of the event (e.g., "orders.created", "stock.reserved").
     */
    @Column(name = "event_type", nullable = false)
    private String eventType;

    /**
     * JSON payload containing the event data.
     * Stored as JSONB in PostgreSQL for efficient querying.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", nullable = false)
    private String payload;

    /**
     * Current status of the event in the publishing lifecycle.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OutboxStatus status;

    /**
     * Timestamp when the event occurred/was created.
     */
    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    /**
     * Timestamp when the event was successfully sent to the message broker.
     * Null if not yet sent or if sending failed.
     */
    @Column(name = "sent_at")
    private Instant sentAt;

    @Column(name = "retry_count")
    @Builder.Default
    private Integer retryCount = 0;

    @Column(name = "last_retry_at")
    private Instant lastRetryAt;

    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    /**
     * Lifecycle callback to set default values before persisting.
     */
    @PrePersist
    void onCreate() {
        if (this.eventId == null) {
            this.eventId = UUID.randomUUID();
        }
        if (this.status == null) {
            this.status = OutboxStatus.NEW;
        }
        if (this.occurredAt == null) {
            this.occurredAt = Instant.now();
        }
    }
}
