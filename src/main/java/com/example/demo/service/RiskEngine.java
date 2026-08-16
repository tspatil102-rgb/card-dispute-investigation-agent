package com.example.demo.service;

import com.example.demo.dto.EvidenceBundle;
import com.example.demo.dto.RiskResult;
import com.example.demo.dto.RiskRule;
import com.example.demo.enums.CardStatus;
import com.example.demo.enums.RiskBand;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;

@Service
public class RiskEngine {
    @Autowired
    private AuditService auditService;

    public RiskResult evaluateRisk(String caseId, EvidenceBundle evidence) {
        int riskScore = 0;
        List<RiskRule> triggeredRules = new ArrayList<>();

        if (evidence.getTransaction() != null && evidence.getCustomerProfile() != null) {
            // Rule 1: Location mismatch
            if (!evidence.getTransaction().getCity().equalsIgnoreCase(evidence.getCustomerProfile().getHomeCity())) {
                riskScore += 30;
                triggeredRules.add(new RiskRule("LOCATION_MISMATCH", 30,
                        "Transaction city " + evidence.getTransaction().getCity() +
                        " differs from customer home city " + evidence.getCustomerProfile().getHomeCity()));
            }

            // Rule 2: Amount anomaly (more than 10x average)
            if (evidence.getCustomerProfile().getAverageTransactionAmount() != null &&
                evidence.getTransaction().getAmount() > (evidence.getCustomerProfile().getAverageTransactionAmount() * 10)) {
                riskScore += 20;
                triggeredRules.add(new RiskRule("AMOUNT_ANOMALY", 20,
                        "Transaction amount is more than 10x average spend"));
            }
        }

        // Rule 3: Unknown merchant
        if (evidence.getTransaction() != null && isMerchantUnknown(evidence.getTransaction().getMerchant())) {
            riskScore += 15;
            triggeredRules.add(new RiskRule("UNKNOWN_MERCHANT", 15,
                    "Merchant is not in customer's typical transaction history"));
        }

        // Rule 4: Card reported lost
        if (evidence.getCardStatus() != null && evidence.getCardStatus().getStatus() == CardStatus.LOST) {
            riskScore += 40;
            triggeredRules.add(new RiskRule("CARD_REPORTED_LOST", 40,
                    "Card status is LOST"));
        }

        // Rule 5: Device mismatch
        if (evidence.getTransaction() != null &&
            "DEVICE_MISMATCH".equals(compareDevice(evidence))) {
            riskScore += 25;
            triggeredRules.add(new RiskRule("DEVICE_MISMATCH", 25,
                    "Transaction from unregistered device"));
        }

        // Rule 6: High-risk merchant category
        if (evidence.getTransaction() != null &&
            evidence.getTransaction().getMerchantCategory() != null &&
            evidence.getTransaction().getMerchantCategory().name().equals("ELECTRONICS")) {
            riskScore += 10; // Moderate increase for electronics
        }

        // Rule 7: Multiple recent disputes
        if (evidence.getPriorDisputes() != null && evidence.getPriorDisputes().size() > 0) {
            riskScore += 20;
            triggeredRules.add(new RiskRule("MULTIPLE_RECENT_DISPUTES", 20,
                    "Customer has " + evidence.getPriorDisputes().size() + " prior dispute(s)"));
        }

        // Cap score at 100
        riskScore = Math.min(riskScore, 100);

        // Determine risk band
        RiskBand riskBand;
        if (riskScore >= 70) {
            riskBand = RiskBand.HIGH;
        } else if (riskScore >= 30) {
            riskBand = RiskBand.MEDIUM;
        } else {
            riskBand = RiskBand.LOW;
        }

        RiskResult result = RiskResult.builder()
                .riskScore(riskScore)
                .riskBand(riskBand)
                .triggeredRules(triggeredRules)
                .build();

        // Audit log
        if (auditService != null) {
            auditService.logStep(caseId, "RISK_EVALUATION", "RiskEngine", "DETERMINISTIC_RULES",
                    "Evidence for merchant: " + (evidence.getTransaction() != null ? evidence.getTransaction().getMerchant() : "N/A"),
                    "Risk score: " + riskScore + ", Band: " + riskBand,
                    null, "V1", riskScore);
        }

        return result;
    }

    private boolean isMerchantUnknown(String merchant) {
        // Mock check - in production would query transaction history
        List<String> knownMerchants = List.of("Amazon", "Flipkart", "Grocery Store", "Electronics World");
        return !knownMerchants.stream()
                .anyMatch(m -> merchant.toLowerCase().contains(m.toLowerCase()));
    }

    private String compareDevice(EvidenceBundle evidence) {
        // Mock device comparison - in production would check against registered devices
        return "DEVICE_MATCH"; // Default to match for this POC
    }
}
