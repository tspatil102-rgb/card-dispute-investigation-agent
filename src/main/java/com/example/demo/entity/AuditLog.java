package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "audit_log")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String caseId;

    @Column(nullable = false)
    private String stepName;

    @Column(nullable = false)
    private String agentName;

    private String toolCalled;

    @Lob
    private String inputSummary;

    @Lob
    private String outputSummary;

    private String modelName;

    private String promptVersion;

    private Integer riskScoreSnapshot;

    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
