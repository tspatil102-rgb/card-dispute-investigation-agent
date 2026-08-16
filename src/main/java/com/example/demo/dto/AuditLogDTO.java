package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLogDTO {
    private Long id;
    private String caseId;
    private String stepName;
    private String agentName;
    private String toolCalled;
    private String inputSummary;
    private String outputSummary;
    private String modelName;
    private String promptVersion;
    private Integer riskScoreSnapshot;
    private LocalDateTime createdAt;
}
