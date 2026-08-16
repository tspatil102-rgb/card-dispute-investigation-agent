package com.example.demo;

import com.example.demo.dto.*;
import com.example.demo.enums.DisputeStatus;
import com.example.demo.service.DisputeService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
public class DisputeInvestigationIntegrationTest {

    @Autowired
    private DisputeService disputeService;

    @Test
    public void testEndToEndDisputeWorkflow() throws Exception {
        // Step 1: Create a dispute case
        CreateDisputeRequest request = new CreateDisputeRequest();
        request.setCustomerId(1002L);
        request.setComplaintText("I did not make a transaction of ₹1800 at Amazon on yesterday");

        DisputeCaseResponseDTO createdCase = disputeService.createDispute(request);
        assertNotNull(createdCase.getCaseId());
        assertEquals(DisputeStatus.NEW, createdCase.getStatus());

        String caseId = createdCase.getCaseId();

        // Step 2: Run investigation
        InvestigationResponse investigationResponse = disputeService.investigateDispute(caseId);
        assertNotNull(investigationResponse);
        assertEquals(DisputeStatus.PENDING_ANALYST_REVIEW, investigationResponse.getStatus());
        assertNotNull(investigationResponse.getRiskResult());
        assertNotNull(investigationResponse.getRecommendation());
        assertTrue(investigationResponse.isAnalystNoteGenerated());
        assertTrue(investigationResponse.isCustomerResponseDraftGenerated());

        // Step 3: Verify risk evaluation
        RiskResult riskResult = investigationResponse.getRiskResult();
        assertNotNull(riskResult);
        assertGreaterThanOrEqual(riskResult.getRiskScore(), 0);
        assertLessThanOrEqual(riskResult.getRiskScore(), 100);

        // Step 4: Verify recommendation
        RecommendationDTO recommendation = investigationResponse.getRecommendation();
        assertNotNull(recommendation.getDecision());
        assertNotNull(recommendation.getConfidence());
        assertNotNull(recommendation.getRecommendedActions());

        // Step 5: Get audit trail
        java.util.List<AuditLogDTO> auditTrail = disputeService.getAuditTrail(caseId);
        assertGreater(auditTrail.size(), 0);

        // Step 6: Get customer response draft
        CustomerResponseDraftDTO responseDraft = disputeService.getCustomerResponseDraft(caseId);
        assertNotNull(responseDraft.getResponseDraft());
        assertEquals("DRAFT", responseDraft.getStatus());

        // Step 7: Analyst review
        ReviewDecisionRequest reviewRequest = new ReviewDecisionRequest();
        reviewRequest.setCaseId(caseId);
        reviewRequest.setDecision("APPROVE");
        reviewRequest.setApprovedBy("analyst@bank.com");

        DisputeCaseResponseDTO reviewedCase = disputeService.reviewDispute(reviewRequest);
        assertEquals(DisputeStatus.APPROVED, reviewedCase.getStatus());
    }

    @Test
    public void testHighRiskCaseScenario() throws Exception {
        // Scenario: Customer 1001 with high-risk transaction (LOST card)
        CreateDisputeRequest request = new CreateDisputeRequest();
        request.setCustomerId(1001L);
        request.setComplaintText("I did not make a ₹75000 transaction at Electronics World");

        DisputeCaseResponseDTO createdCase = disputeService.createDispute(request);
        String caseId = createdCase.getCaseId();

        // Run investigation
        InvestigationResponse response = disputeService.investigateDispute(caseId);

        // Verify high risk
        assertGreaterThanOrEqual(response.getRiskResult().getRiskScore(), 40); // Card LOST = 40
        assertNotNull(response.getRecommendation().getDecision());
    }

    @Test
    public void testLowRiskCaseScenario() throws Exception {
        // Scenario: Customer 1002 with legitimate transaction
        CreateDisputeRequest request = new CreateDisputeRequest();
        request.setCustomerId(1002L);
        request.setComplaintText("I don't recognize a ₹1800 transaction at Amazon");

        DisputeCaseResponseDTO createdCase = disputeService.createDispute(request);
        String caseId = createdCase.getCaseId();

        // Run investigation
        InvestigationResponse response = disputeService.investigateDispute(caseId);

        // Verify low risk
        assertLessThan(response.getRiskResult().getRiskScore(), 30);
        assertNotNull(response.getRecommendation());
    }

    @Test
    public void testGetTimeline() throws Exception {
        // Create and investigate a case
        CreateDisputeRequest request = new CreateDisputeRequest();
        request.setCustomerId(1003L);
        request.setComplaintText("Suspicious transaction of ₹2500");

        DisputeCaseResponseDTO createdCase = disputeService.createDispute(request);
        String caseId = createdCase.getCaseId();
        disputeService.investigateDispute(caseId);

        // Get timeline
        java.util.List<TimelineEventDTO> timeline = disputeService.getTimeline(caseId);
        assertGreater(timeline.size(), 0);
    }

    private void assertGreater(int actual, int expected) {
        assertTrue(actual > expected, "Expected " + actual + " to be greater than " + expected);
    }

    private void assertGreaterThanOrEqual(int actual, int expected) {
        assertTrue(actual >= expected, "Expected " + actual + " to be >= " + expected);
    }

    private void assertLessThan(int actual, int expected) {
        assertTrue(actual < expected, "Expected " + actual + " to be less than " + expected);
    }

    private void assertLessThanOrEqual(int actual, int expected) {
        assertTrue(actual <= expected, "Expected " + actual + " to be <= " + expected);
    }
}
