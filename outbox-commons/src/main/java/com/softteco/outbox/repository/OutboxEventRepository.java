package com.softteco.outbox.repository;

import com.softteco.outbox.entity.OutboxEventEntity;
import com.softteco.outbox.entity.OutboxStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Repository for accessing and managing outbox events.
 */
@Repository
public interface OutboxEventRepository extends JpaRepository<OutboxEventEntity, UUID> {

    /**
     * Find a batch of events with the specified status, ordered by occurrence time.
     */
    @Query("SELECT e FROM OutboxEventEntity e WHERE e.status = :status ORDER BY e.occurredAt ASC LIMIT :limit")
    List<OutboxEventEntity> findBatch(@Param("status") OutboxStatus status, @Param("limit") int limit);

    /**
     * Delete events that have been successfully sent and are older than the
     * specified time.
     */
    @Modifying
    @Query("DELETE FROM OutboxEventEntity e WHERE e.status = :status AND e.sentAt < :before")
    int deleteByStatusAndSentAtBefore(@Param("status") OutboxStatus status, @Param("before") Instant before);

    /**
     * Count events with the specified status.
     */
    long countByStatus(OutboxStatus status);

    /**
     * Find events by status ordered by occurrence time.
     */
    List<OutboxEventEntity> findByStatusOrderByOccurredAtAsc(OutboxStatus status, Pageable pageable);

    /**
     * Count events stuck in NEW status for longer than the specified duration.
     */
    long countByStatusAndOccurredAtBefore(OutboxStatus status, Instant before);
}
