package com.example.demo.controller;

import com.example.demo.dto.*;
import com.example.demo.service.DisputeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/disputes")
@Tag(name = "Dispute Management", description = "APIs for managing card dispute cases")
public class DisputeController {
    @Autowired
    private DisputeService disputeService;

    @GetMapping
    @Operation(summary = "List all dispute cases")
    public ResponseEntity<List<DisputeCaseResponseDTO>> getAllDisputes() {
        List<DisputeCaseResponseDTO> response = disputeService.getAllDisputes();
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PostMapping
    @Operation(summary = "Create a new dispute case")
    public ResponseEntity<Map<String, Object>> createDispute(@Valid @RequestBody CreateDisputeRequest request) {
        DisputeCaseResponseDTO response = disputeService.createDispute(request);
        Map<String, Object> payload = Map.of(
                "caseId", response.getCaseId(),
                "status", response.getStatus(),
                "customerId", response.getCustomerId(),
                "createdAt", response.getCreatedAt()
        );
        return new ResponseEntity<>(payload, HttpStatus.CREATED);
    }

    @PostMapping("/{caseId}/investigate")
    @Operation(summary = "Investigate a dispute case")
    public ResponseEntity<InvestigationResponse> investigateDispute(@PathVariable String caseId) {
        try {
            InvestigationResponse response = disputeService.investigateDispute(caseId);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/{caseId}")
    @Operation(summary = "Get dispute case details")
    public ResponseEntity<DisputeCaseResponseDTO> getDispute(@PathVariable String caseId) {
        try {
            DisputeCaseResponseDTO response = disputeService.getDispute(caseId);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @PostMapping("/{caseId}/review")
    @Operation(summary = "Submit analyst review decision")
    public ResponseEntity<DisputeCaseResponseDTO> reviewDispute(@PathVariable String caseId,
                                                                 @RequestBody ReviewDecisionRequest request) {
        try {
            request.setCaseId(caseId);
            DisputeCaseResponseDTO response = disputeService.reviewDispute(request);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping("/{caseId}/audit")
    @Operation(summary = "Get audit trail for a case")
    public ResponseEntity<List<AuditLogDTO>> getAuditTrail(@PathVariable String caseId) {
        try {
            List<AuditLogDTO> logs = disputeService.getAuditTrail(caseId);
            return new ResponseEntity<>(logs, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @GetMapping("/{caseId}/timeline")
    @Operation(summary = "Get timeline of events for a case")
    public ResponseEntity<List<TimelineEventDTO>> getTimeline(@PathVariable String caseId) {
        try {
            List<TimelineEventDTO> timeline = disputeService.getTimeline(caseId);
            return new ResponseEntity<>(timeline, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @GetMapping("/{caseId}/customer-response")
    @Operation(summary = "Get customer response draft")
    public ResponseEntity<CustomerResponseDraftDTO> getCustomerResponseDraft(@PathVariable String caseId) {
        try {
            CustomerResponseDraftDTO response = disputeService.getCustomerResponseDraft(caseId);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }
}
