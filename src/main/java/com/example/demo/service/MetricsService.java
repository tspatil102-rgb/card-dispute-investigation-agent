package com.example.demo.service;

import com.example.demo.dto.DisputeMetricsDTO;
import com.example.demo.entity.DisputeCase;
import com.example.demo.enums.DisputeStatus;
import com.example.demo.enums.RiskBand;
import com.example.demo.repository.DisputeCaseRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class MetricsService {

    @Autowired
    private DisputeCaseRepository disputeCaseRepository;

    public DisputeMetricsDTO calculateMetrics() {
        List<DisputeCase> allCases = disputeCaseRepository.findAll();
        
        if (allCases.isEmpty()) {
            return DisputeMetricsDTO.builder()
                    .totalCases(0)
                    .totalApproved(0)
                    .totalClosed(0)
                    .totalEscalated(0)
                    .totalPending(0)
                    .averageRiskScore(0.0)
                    .averageProcessingTimeSeconds(0.0)
                    .highRiskCases(0)
                    .mediumRiskCases(0)
                    .lowRiskCases(0)
                    .generatedAt(LocalDateTime.now())
                    .build();
        }

        int totalCases = allCases.size();
        int approved = (int) allCases.stream()
                .filter(c -> c.getStatus() == DisputeStatus.APPROVED)
                .count();
        int closed = (int) allCases.stream()
                .filter(c -> c.getStatus() == DisputeStatus.CLOSED)
                .count();
        int escalated = (int) allCases.stream()
                .filter(c -> c.getStatus() == DisputeStatus.ESCALATED)
                .count();
        int pending = (int) allCases.stream()
                .filter(c -> c.getStatus() == DisputeStatus.PENDING_ANALYST_REVIEW)
                .count();

        double averageRiskScore = allCases.stream()
                .filter(c -> c.getRiskScore() != null)
                .mapToInt(DisputeCase::getRiskScore)
                .average()
                .orElse(0.0);

        double averageProcessingTime = allCases.stream()
                .filter(c -> c.getCreatedAt() != null && c.getUpdatedAt() != null)
                .mapToLong(c -> java.time.temporal.ChronoUnit.SECONDS
                        .between(c.getCreatedAt(), c.getUpdatedAt()))
                .average()
                .orElse(0.0);

        int highRiskCases = (int) allCases.stream()
                .filter(c -> c.getRiskBand() == RiskBand.HIGH)
                .count();
        int mediumRiskCases = (int) allCases.stream()
                .filter(c -> c.getRiskBand() == RiskBand.MEDIUM)
                .count();
        int lowRiskCases = (int) allCases.stream()
                .filter(c -> c.getRiskBand() == RiskBand.LOW)
                .count();

        return DisputeMetricsDTO.builder()
                .totalCases(totalCases)
                .totalApproved(approved)
                .totalClosed(closed)
                .totalEscalated(escalated)
                .totalPending(pending)
                .averageRiskScore(averageRiskScore)
                .averageProcessingTimeSeconds(averageProcessingTime)
                .highRiskCases(highRiskCases)
                .mediumRiskCases(mediumRiskCases)
                .lowRiskCases(lowRiskCases)
                .generatedAt(LocalDateTime.now())
                .build();
    }
}
