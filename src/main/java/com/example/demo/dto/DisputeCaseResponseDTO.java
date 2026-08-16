package com.example.demo.dto;

import com.example.demo.enums.DisputeStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DisputeCaseResponseDTO {
    private String caseId;
    private DisputeStatus status;
    private Long customerId;
    private Integer riskScore;
    private String riskBand;
    private RecommendationDTO recommendation;
    private String analystNote;
    private String customerResponseDraft;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
