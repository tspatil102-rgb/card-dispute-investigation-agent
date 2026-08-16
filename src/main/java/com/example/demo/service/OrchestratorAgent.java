package com.example.demo.service;

import com.example.demo.dto.*;
import com.example.demo.entity.CustomerResponseDraft;
import com.example.demo.entity.DisputeCase;
import com.example.demo.entity.TimelineEvent;
import com.example.demo.enums.DisputeStatus;
import com.example.demo.enums.TimelineEventType;
import com.example.demo.repository.CustomerResponseDraftRepository;
import com.example.demo.repository.DisputeCaseRepository;
import com.example.demo.repository.TimelineEventRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class OrchestratorAgent {
    @Autowired
    private DisputeCaseRepository disputeCaseRepository;

    @Autowired
    private TimelineEventRepository timelineEventRepository;

    @Autowired
    private IntakeAgent intakeAgent;

    @Autowired
    private InvestigationAgent investigationAgent;

    @Autowired
    private RiskEngine riskEngine;

    @Autowired
    private DecisionRecommendationAgent decisionRecommendationAgent;

    @Autowired
    private AnalystNoteGenerator analystNoteGenerator;

    @Autowired
    private CustomerResponseGenerator customerResponseGenerator;

    @Autowired
    private CustomerResponseDraftRepository customerResponseDraftRepository;

    @Autowired
    private AuditService auditService;

    public InvestigationResponse investigateDispute(String caseId) throws Exception {
        long startTime = System.currentTimeMillis();

        // Step 1: Load dispute case
        Optional<DisputeCase> caseOpt = disputeCaseRepository.findByCaseId(caseId);
        if (!caseOpt.isPresent()) {
            throw new Exception("Case not found: " + caseId);
        }

        DisputeCase disputeCase = caseOpt.get();
        logTimelineEvent(caseId, TimelineEventType.INTAKE_STARTED, "Starting intake process",
                DisputeStatus.NEW, null);
        auditService.logStep(caseId, "ORCHESTRATION_START", "OrchestratorAgent", "CASE_LOAD",
                "Started investigation for case", "Case loaded", null, "V1", null);

        // Step 2: Extract complaint details
        ComplaintExtractDTO complaintExtract = intakeAgent.extractComplaintDetails(caseId, disputeCase.getComplaintText());
        disputeCase.setStatus(DisputeStatus.INTAKE_COMPLETED);
        disputeCaseRepository.save(disputeCase);
        logTimelineEvent(caseId, TimelineEventType.INTAKE_COMPLETED, "Complaint details extracted",
                DisputeStatus.INTAKE_COMPLETED,
                System.currentTimeMillis() - startTime);

        // Step 3: Gather evidence
        long investigationStart = System.currentTimeMillis();
        logTimelineEvent(caseId, TimelineEventType.INVESTIGATION_STARTED, "Starting evidence gathering",
                DisputeStatus.INTAKE_COMPLETED, null);
        EvidenceBundle evidence = investigationAgent.investigateDispute(caseId, complaintExtract, disputeCase.getCustomerId());
        disputeCase.setStatus(DisputeStatus.EVIDENCE_COLLECTED);
        disputeCaseRepository.save(disputeCase);
        logTimelineEvent(caseId, TimelineEventType.INVESTIGATION_COMPLETED, "Evidence bundle created",
                DisputeStatus.EVIDENCE_COLLECTED,
                System.currentTimeMillis() - investigationStart);

        // Step 4: Evaluate risk
        long riskStart = System.currentTimeMillis();
        logTimelineEvent(caseId, TimelineEventType.RISK_EVALUATION_STARTED, "Starting risk evaluation",
                DisputeStatus.EVIDENCE_COLLECTED, null);
        RiskResult riskResult = riskEngine.evaluateRisk(caseId, evidence);
        disputeCase.setRiskScore(riskResult.getRiskScore());
        disputeCase.setRiskBand(riskResult.getRiskBand());
        disputeCase.setStatus(DisputeStatus.RISK_EVALUATED);
        disputeCaseRepository.save(disputeCase);
        logTimelineEvent(caseId, TimelineEventType.RISK_EVALUATION_COMPLETED,
                "Risk score: " + riskResult.getRiskScore() + ", Band: " + riskResult.getRiskBand(),
                DisputeStatus.RISK_EVALUATED,
                System.currentTimeMillis() - riskStart);

        // Step 5: Generate recommendation
        RecommendationDTO recommendation = decisionRecommendationAgent.generateRecommendation(caseId, evidence, riskResult);
        disputeCase.setRecommendedDecision(recommendation.getDecision());
        disputeCase.setRecommendationReason(recommendation.getReason());
        disputeCase.setStatus(DisputeStatus.RECOMMENDATION_GENERATED);
        disputeCaseRepository.save(disputeCase);
        logTimelineEvent(caseId, TimelineEventType.RECOMMENDATION_GENERATED,
                "Recommendation: " + recommendation.getDecision(),
                DisputeStatus.RECOMMENDATION_GENERATED, null);

        // Step 6: Generate analyst note
        String analystNote = analystNoteGenerator.generateAnalystNote(caseId, evidence, riskResult, recommendation);
        disputeCase.setAnalystNote(analystNote);
        logTimelineEvent(caseId, TimelineEventType.ANALYST_NOTE_GENERATED, "Analyst note generated",
                DisputeStatus.RECOMMENDATION_GENERATED, null);

        // Step 7: Generate customer response draft
        String customerResponse = customerResponseGenerator.generateCustomerResponse(caseId, evidence, recommendation);
        disputeCase.setCustomerResponseDraft(customerResponse);

        // Save customer response draft separately
        CustomerResponseDraft responseDraft = CustomerResponseDraft.builder()
                .caseId(caseId)
                .responseDraft(customerResponse)
                .status("DRAFT")
                .createdAt(LocalDateTime.now())
                .build();
        customerResponseDraftRepository.save(responseDraft);
        logTimelineEvent(caseId, TimelineEventType.CUSTOMER_RESPONSE_GENERATED, "Customer response drafted",
                DisputeStatus.RECOMMENDATION_GENERATED, null);

        // Step 8: Move case to analyst review
        disputeCase.setStatus(DisputeStatus.PENDING_ANALYST_REVIEW);
        disputeCase.setUpdatedAt(LocalDateTime.now());
        disputeCaseRepository.save(disputeCase);
        logTimelineEvent(caseId, TimelineEventType.PENDING_ANALYST_REVIEW, "Case ready for analyst review",
                DisputeStatus.PENDING_ANALYST_REVIEW,
                System.currentTimeMillis() - startTime);

        auditService.logStep(caseId, "ORCHESTRATION_COMPLETE", "OrchestratorAgent", "CASE_READY_FOR_REVIEW",
                "Investigation completed", "Case moved to analyst review", null, "V1", riskResult.getRiskScore());

        // Return investigation response
        return InvestigationResponse.builder()
                .caseId(caseId)
                .status(DisputeStatus.PENDING_ANALYST_REVIEW)
                .riskResult(riskResult)
                .recommendation(recommendation)
                .analystNoteGenerated(true)
                .customerResponseDraftGenerated(true)
                .updatedAt(LocalDateTime.now())
                .build();
    }

    private void logTimelineEvent(String caseId, TimelineEventType eventType, String description,
                                  DisputeStatus status, Long duration) {
        TimelineEvent event = TimelineEvent.builder()
                .caseId(caseId)
                .eventType(eventType.toString())
                .description(description)
                .statusAtEvent(status)
                .duration(duration)
                .build();
        timelineEventRepository.save(event);    }
}