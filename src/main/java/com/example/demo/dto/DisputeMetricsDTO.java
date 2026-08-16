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
public class DisputeMetricsDTO {
    private Integer totalCases;
    private Integer totalApproved;
    private Integer totalClosed;
    private Integer totalEscalated;
    private Integer totalPending;
    private Double averageRiskScore;
    private Double averageProcessingTimeSeconds;
    private Integer highRiskCases;
    private Integer mediumRiskCases;
    private Integer lowRiskCases;
    private LocalDateTime generatedAt;
    
    // Calculated percentages
    public Double getApprovedPercentage() {
        return totalCases > 0 ? (totalApproved * 100.0) / totalCases : 0;
    }
    
    public Double getClosedPercentage() {
        return totalCases > 0 ? (totalClosed * 100.0) / totalCases : 0;
    }
    
    public Double getEscalatedPercentage() {
        return totalCases > 0 ? (totalEscalated * 100.0) / totalCases : 0;
    }
}
