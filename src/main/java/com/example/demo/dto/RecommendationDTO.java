package com.example.demo.dto;

import com.example.demo.enums.RecommendationDecision;
import com.example.demo.enums.RecommendedAction;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecommendationDTO {
    private RecommendationDecision decision;
    private String confidence;
    private Integer riskScore;
    private String riskBand;
    private List<RecommendedAction> recommendedActions;
    private String reason;
}
