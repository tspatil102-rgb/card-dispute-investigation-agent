package com.example.demo.service;

import com.example.demo.dto.*;
import com.example.demo.entity.*;
import com.example.demo.enums.CardStatus;
import com.example.demo.enums.DisputeStatus;
import com.example.demo.enums.RiskTier;
import com.example.demo.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class DisputeService {
    @Autowired
    private DisputeCaseRepository disputeCaseRepository;

    @Autowired
    private ComplaintExtractRepository complaintExtractRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private CustomerResponseDraftRepository customerResponseDraftRepository;

    @Autowired
    private OrchestratorAgent orchestratorAgent;

    public List<DisputeCaseResponseDTO> getAllDisputes() {
        return disputeCaseRepository.findAll()
                .stream()
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());
    }

    public DisputeCaseResponseDTO createDispute(CreateDisputeRequest request) {
        // Generate case ID
        String caseId = "D" + System.currentTimeMillis();

        // Create dispute case
        DisputeCase disputeCase = DisputeCase.builder()
                .caseId(caseId)
                .status(DisputeStatus.NEW)
                .complaintText(request.getComplaintText())
                .customerId(request.getCustomerId())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        DisputeCase savedCase = disputeCaseRepository.save(disputeCase);

        return mapToResponseDTO(savedCase);
    }

    public InvestigationResponse investigateDispute(String caseId) throws Exception {
        return orchestratorAgent.investigateDispute(caseId);
    }

    public DisputeCaseResponseDTO getDispute(String caseId) {
        Optional<DisputeCase> caseOpt = disputeCaseRepository.findByCaseId(caseId);
        if (caseOpt.isPresent()) {
            return mapToResponseDTO(caseOpt.get());
        }
        throw new RuntimeException("Case not found: " + caseId);
    }

    public DisputeCaseResponseDTO reviewDispute(ReviewDecisionRequest request) {
        Optional<DisputeCase> caseOpt = disputeCaseRepository.findByCaseId(request.getCaseId());
        if (!caseOpt.isPresent()) {
            throw new RuntimeException("Case not found: " + request.getCaseId());
        }

        DisputeCase disputeCase = caseOpt.get();

        // Update case with final decision
        if ("APPROVE".equalsIgnoreCase(request.getDecision())) {
            disputeCase.setStatus(DisputeStatus.APPROVED);
        } else if ("CLOSE".equalsIgnoreCase(request.getDecision())) {
            disputeCase.setStatus(DisputeStatus.CLOSED);
        } else if ("ESCALATE".equalsIgnoreCase(request.getDecision())) {
            disputeCase.setStatus(DisputeStatus.ESCALATED);
        }

        disputeCase.setFinalDecision(request.getDecision());
        disputeCase.setFinalDecisionBy(request.getApprovedBy());
        disputeCase.setFinalDecisionAt(LocalDateTime.now());
        disputeCase.setUpdatedAt(LocalDateTime.now());

        DisputeCase saved = disputeCaseRepository.save(disputeCase);
        return mapToResponseDTO(saved);
    }

    public List<AuditLogDTO> getAuditTrail(String caseId) {
        return auditLogRepository.findByCaseIdOrderByCreatedAtDesc(caseId)
                .stream()
                .map(log -> AuditLogDTO.builder()
                        .id(log.getId())
                        .caseId(log.getCaseId())
                        .stepName(log.getStepName())
                        .agentName(log.getAgentName())
                        .toolCalled(log.getToolCalled())
                        .inputSummary(log.getInputSummary())
                        .outputSummary(log.getOutputSummary())
                        .modelName(log.getModelName())
                        .promptVersion(log.getPromptVersion())
                        .riskScoreSnapshot(log.getRiskScoreSnapshot())
                        .createdAt(log.getCreatedAt())
                        .build())
                .collect(Collectors.toList());
    }

    public CustomerResponseDraftDTO getCustomerResponseDraft(String caseId) {
        Optional<CustomerResponseDraft> draftOpt = customerResponseDraftRepository.findByCaseId(caseId);
        if (draftOpt.isPresent()) {
            CustomerResponseDraft draft = draftOpt.get();
            return CustomerResponseDraftDTO.builder()
                    .caseId(draft.getCaseId())
                    .responseDraft(draft.getResponseDraft())
                    .status(draft.getStatus())
                    .createdAt(draft.getCreatedAt())
                    .approvedAt(draft.getApprovedAt())
                    .approvedBy(draft.getApprovedBy())
                    .build();
        }
        throw new RuntimeException("Response draft not found for case: " + caseId);
    }

    public List<TimelineEventDTO> getTimeline(String caseId) {
        Optional<DisputeCase> caseOpt = disputeCaseRepository.findByCaseId(caseId);
        if (!caseOpt.isPresent()) {
            throw new RuntimeException("Case not found: " + caseId);
        }

        DisputeCase dc = caseOpt.get();
        List<TimelineEventDTO> timeline = List.of(
            TimelineEventDTO.builder()
                    .timestamp(dc.getCreatedAt())
                    .event("Dispute case created")
                    .status(DisputeStatus.NEW.toString())
                    .build(),
            TimelineEventDTO.builder()
                    .timestamp(dc.getUpdatedAt())
                    .event("Investigation completed")
                    .status(DisputeStatus.PENDING_ANALYST_REVIEW.toString())
                    .build()
        );

        return timeline;
    }

    public DisputeCaseResponseDTO mapToResponseDTO(DisputeCase entity) {
        return DisputeCaseResponseDTO.builder()
                .caseId(entity.getCaseId())
                .status(entity.getStatus())
                .customerId(entity.getCustomerId())
                .riskScore(entity.getRiskScore())
                .riskBand(entity.getRiskBand() != null ? entity.getRiskBand().toString() : null)
                .analystNote(entity.getAnalystNote())
                .customerResponseDraft(entity.getCustomerResponseDraft())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
