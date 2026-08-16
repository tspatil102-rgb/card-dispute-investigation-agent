package com.example.demo.service;

import com.example.demo.entity.AuditLog;
import com.example.demo.repository.AuditLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class AuditService {
    @Autowired
    private AuditLogRepository auditLogRepository;

    public void logStep(String caseId, String stepName, String agentName, String toolCalled,
                        String inputSummary, String outputSummary, String modelName,
                        String promptVersion, Integer riskScoreSnapshot) {
        AuditLog log = AuditLog.builder()
                .caseId(caseId)
                .stepName(stepName)
                .agentName(agentName)
                .toolCalled(toolCalled)
                .inputSummary(inputSummary)
                .outputSummary(outputSummary)
                .modelName(modelName)
                .promptVersion(promptVersion)
                .riskScoreSnapshot(riskScoreSnapshot)
                .createdAt(LocalDateTime.now())
                .build();

        auditLogRepository.save(log);
    }

    public List<AuditLog> getAuditTrail(String caseId) {
        return auditLogRepository.findByCaseIdOrderByCreatedAtDesc(caseId);
    }
}
