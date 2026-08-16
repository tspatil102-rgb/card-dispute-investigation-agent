package com.example.demo.dto;

import com.example.demo.enums.DisputeStatus;
import com.example.demo.enums.RiskBand;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvestigationResponse {
    private String caseId;
    private DisputeStatus status;
    private RiskResult riskResult;
    private RecommendationDTO recommendation;
    private boolean analystNoteGenerated;
    private boolean customerResponseDraftGenerated;
    private LocalDateTime updatedAt;
}
