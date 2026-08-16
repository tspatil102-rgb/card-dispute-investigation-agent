package com.example.demo.entity;

import com.example.demo.enums.DisputeStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "timeline_event")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TimelineEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String caseId;

    @Column(nullable = false)
    private String eventType; // CASE_CREATED, INTAKE_STARTED, INVESTIGATION_STARTED, RISK_EVALUATED, etc.

    @Column(length = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    private DisputeStatus statusAtEvent;

    private Long duration; // Duration in milliseconds if applicable

    @Column(columnDefinition = "TEXT")
    private String details; // JSON with additional event details

    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
