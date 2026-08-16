package com.example.demo.service;

import com.example.demo.dto.*;
import com.example.demo.enums.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.context.SpringBootTest;
import static org.junit.jupiter.api.Assertions.*;
import java.util.ArrayList;
import java.util.List;

@SpringBootTest
public class RiskEngineTest {

    private RiskEngine riskEngine;
    private EvidenceBundle evidenceBundle;

    @BeforeEach
    public void setUp() {
        riskEngine = new RiskEngine(); // AuditService is optional for this test
        
        // Create test evidence bundle
        evidenceBundle = EvidenceBundle.builder()
                .caseId("TEST001")
                .build();
    }

    @Test
    public void testNoRiskFactors() {
        // Setup: Customer in home city, normal amount, active card
        CustomerProfileDTO profile = CustomerProfileDTO.builder()
                .customerId(1L)
                .homeCity("Mumbai")
                .averageTransactionAmount(5000.0)
                .riskTier(RiskTier.LOW)
                .build();

        TransactionDTO transaction = TransactionDTO.builder()
                .transactionId("TXN001")
                .city("Mumbai")
                .amount(5000.0)
                .merchant("Local Store")
                .merchantCategory(MerchantCategory.GROCERY)
                .build();

        CardStatusDTO cardStatus = new CardStatusDTO(CardStatus.ACTIVE);

        evidenceBundle.setCustomerProfile(profile);
        evidenceBundle.setTransaction(transaction);
        evidenceBundle.setCardStatus(cardStatus);
        evidenceBundle.setPriorDisputes(new ArrayList<>());

        // Execute
        RiskResult result = riskEngine.evaluateRisk("TEST001", evidenceBundle);

        // Assert
        assertEquals(0, result.getRiskScore());
        assertEquals(RiskBand.LOW, result.getRiskBand());
        assertEquals(0, result.getTriggeredRules().size());
    }

    @Test
    public void testLocationMismatch() {
        CustomerProfileDTO profile = CustomerProfileDTO.builder()
                .customerId(1L)
                .homeCity("Mumbai")
                .averageTransactionAmount(5000.0)
                .build();

        TransactionDTO transaction = TransactionDTO.builder()
                .transactionId("TXN002")
                .city("Dubai")
                .amount(5000.0)
                .merchant("Electronics Store")
                .merchantCategory(MerchantCategory.ELECTRONICS)
                .build();

        CardStatusDTO cardStatus = new CardStatusDTO(CardStatus.ACTIVE);

        evidenceBundle.setCustomerProfile(profile);
        evidenceBundle.setTransaction(transaction);
        evidenceBundle.setCardStatus(cardStatus);
        evidenceBundle.setPriorDisputes(new ArrayList<>());

        // Execute
        RiskResult result = riskEngine.evaluateRisk("TEST002", evidenceBundle);

        // Assert
        assertGreaterThan(result.getRiskScore(), 0);
        assertTrue(result.getTriggeredRules().stream()
                .anyMatch(rule -> rule.getRuleCode().equals("LOCATION_MISMATCH")));
    }

    @Test
    public void testCardLostHighRisk() {
        CustomerProfileDTO profile = CustomerProfileDTO.builder()
                .customerId(1L)
                .homeCity("Mumbai")
                .averageTransactionAmount(2000.0)
                .build();

        TransactionDTO transaction = TransactionDTO.builder()
                .transactionId("TXN003")
                .city("Mumbai")
                .amount(75000.0) // Large amount
                .merchant("Electronics")
                .build();

        CardStatusDTO cardStatus = new CardStatusDTO(CardStatus.LOST);

        evidenceBundle.setCustomerProfile(profile);
        evidenceBundle.setTransaction(transaction);
        evidenceBundle.setCardStatus(cardStatus);
        evidenceBundle.setPriorDisputes(new ArrayList<>());

        // Execute
        RiskResult result = riskEngine.evaluateRisk("TEST003", evidenceBundle);

        // Assert
        assertGreaterThanOrEqual(result.getRiskScore(), 40); // CARD_REPORTED_LOST = 40
        assertTrue(result.getTriggeredRules().stream()
                .anyMatch(rule -> rule.getRuleCode().equals("CARD_REPORTED_LOST")));
    }

    @Test
    public void testAmountAnomaly() {
        CustomerProfileDTO profile = CustomerProfileDTO.builder()
                .customerId(1L)
                .homeCity("Mumbai")
                .averageTransactionAmount(1000.0)
                .build();

        TransactionDTO transaction = TransactionDTO.builder()
                .transactionId("TXN004")
                .city("Mumbai")
                .amount(15000.0) // 15x average
                .merchant("Electronics")
                .build();

        CardStatusDTO cardStatus = new CardStatusDTO(CardStatus.ACTIVE);

        evidenceBundle.setCustomerProfile(profile);
        evidenceBundle.setTransaction(transaction);
        evidenceBundle.setCardStatus(cardStatus);
        evidenceBundle.setPriorDisputes(new ArrayList<>());

        // Execute
        RiskResult result = riskEngine.evaluateRisk("TEST004", evidenceBundle);

        // Assert
        assertGreaterThan(result.getRiskScore(), 0);
        assertTrue(result.getTriggeredRules().stream()
                .anyMatch(rule -> rule.getRuleCode().equals("AMOUNT_ANOMALY")));
    }

    @Test
    public void testMultiplePriorDisputes() {
        CustomerProfileDTO profile = CustomerProfileDTO.builder()
                .customerId(1L)
                .homeCity("Mumbai")
                .averageTransactionAmount(5000.0)
                .build();

        TransactionDTO transaction = TransactionDTO.builder()
                .transactionId("TXN005")
                .city("Mumbai")
                .amount(5000.0)
                .merchant("Store")
                .build();

        CardStatusDTO cardStatus = new CardStatusDTO(CardStatus.ACTIVE);

        List<PriorDisputeDTO> disputes = new ArrayList<>();
        disputes.add(PriorDisputeDTO.builder().caseId("D001").status("CONFIRMED_FRAUD").build());
        disputes.add(PriorDisputeDTO.builder().caseId("D002").status("APPROVED").build());

        evidenceBundle.setCustomerProfile(profile);
        evidenceBundle.setTransaction(transaction);
        evidenceBundle.setCardStatus(cardStatus);
        evidenceBundle.setPriorDisputes(disputes);

        // Execute
        RiskResult result = riskEngine.evaluateRisk("TEST005", evidenceBundle);

        // Assert
        assertGreaterThan(result.getRiskScore(), 0);
        assertTrue(result.getTriggeredRules().stream()
                .anyMatch(rule -> rule.getRuleCode().equals("MULTIPLE_RECENT_DISPUTES")));
    }

    @Test
    public void testRiskBandMapping() {
        // Test LOW band (0-29)
        assertEquals(RiskBand.LOW, getRiskBand(15));
        
        // Test MEDIUM band (30-69)
        assertEquals(RiskBand.MEDIUM, getRiskBand(50));
        
        // Test HIGH band (70-100)
        assertEquals(RiskBand.HIGH, getRiskBand(85));
    }

    private RiskBand getRiskBand(int score) {
        if (score < 30) return RiskBand.LOW;
        if (score < 70) return RiskBand.MEDIUM;
        return RiskBand.HIGH;
    }

    private void assertGreaterThan(int actual, int expected) {
        assertTrue(actual > expected, "Expected " + actual + " to be greater than " + expected);
    }

    private void assertGreaterThanOrEqual(int actual, int expected) {
        assertTrue(actual >= expected, "Expected " + actual + " to be >= " + expected);
    }
}
