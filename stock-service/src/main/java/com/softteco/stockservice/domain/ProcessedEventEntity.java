package com.softteco.stockservice.domain;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "processed_events")
public class ProcessedEventEntity {
    @EmbeddedId
    private ProcessedEventId id;
    private Instant processedAt;
}
