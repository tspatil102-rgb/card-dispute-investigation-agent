package com.example.demo.entity;

import com.example.demo.enums.*;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "dispute_case")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DisputeCase {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String caseId;

    @Enumerated(EnumType.STRING)
    private DisputeStatus status;

    @Column(nullable = false)
    private String complaintText;

    @Column(nullable = false)
    private Long customerId;

    private Integer riskScore;

    @Enumerated(EnumType.STRING)
    private RiskBand riskBand;

    @Enumerated(EnumType.STRING)
    private RecommendationDecision recommendedDecision;

    private String recommendationReason;

    @Lob
    private String analystNote;

    @Lob
    private String customerResponseDraft;

    private String finalDecision;
    private String finalDecisionBy;
    private LocalDateTime finalDecisionAt;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
