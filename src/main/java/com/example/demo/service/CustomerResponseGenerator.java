package com.example.demo.service;

import com.example.demo.dto.EvidenceBundle;
import com.example.demo.dto.RecommendationDTO;
import dev.langchain4j.model.chat.ChatLanguageModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CustomerResponseGenerator {
    @Autowired
    private AuditService auditService;

    @Autowired(required = false)
    private ChatLanguageModel chatLanguageModel;

    public String generateCustomerResponse(String caseId, EvidenceBundle evidence, RecommendationDTO recommendation) {
        String response = null;
        String modelName = null;

        // Try LLM-based generation if available
        if (chatLanguageModel != null) {
            response = tryLlmCustomerResponse(evidence, recommendation);
            if (response != null) {
                modelName = "gpt-3.5-turbo";
            }
        }

        // Fallback to template-based generation
        if (response == null) {
            response = generateTemplateCustomerResponse(caseId, evidence, recommendation);
        }

        // Audit log
        auditService.logStep(caseId, "CUSTOMER_RESPONSE", "CustomerResponseGenerator",
                modelName != null ? "LLM_GENERATION" : "TEMPLATE_GENERATION",
                "Generated customer response",
                "Draft response created",
                modelName, "V1", null);

        return response;
    }

    private String tryLlmCustomerResponse(EvidenceBundle evidence, RecommendationDTO recommendation) {
        try {
            String prompt = buildCustomerResponsePrompt(evidence, recommendation);
            return chatLanguageModel.generate(prompt);
        } catch (Exception e) {
            System.err.println("LLM customer response generation failed: " + e.getMessage());
            return null;
        }
    }

    private String buildCustomerResponsePrompt(EvidenceBundle evidence, RecommendationDTO recommendation) {
        String transactionInfo = "N/A";
        if (evidence.getTransaction() != null) {
            transactionInfo = String.format("₹%s at %s in %s",
                    evidence.getTransaction().getAmount(),
                    evidence.getTransaction().getMerchant(),
                    evidence.getTransaction().getCity());
        }

        return String.format("""
You are a banking customer service representative writing a professional, empathetic dispute resolution response.
Write a brief, clear customer-facing response (150-200 words).
Be professional, friendly, and reassuring.
Do not mention internal investigation details.

Transaction: %s
Investigation Result: %s
Confidence: %s

Write the customer response letter:
""",
            transactionInfo,
            recommendation.getDecision(),
            recommendation.getConfidence()
        );
    }
    private String generateTemplateCustomerResponse(String caseId, EvidenceBundle evidence, RecommendationDTO recommendation) {
        StringBuilder response = new StringBuilder();
        response.append("Dear Valued Customer,\n\n");
        response.append("Thank you for reporting the disputed transaction. We have thoroughly investigated your case.\n\n");

        if (evidence.getTransaction() != null) {
            response.append("Transaction: ₹").append(evidence.getTransaction().getAmount()).append(" at ")
                    .append(evidence.getTransaction().getMerchant()).append("\n\n");
        }

        switch (recommendation.getDecision().toString()) {
            case "APPROVE_DISPUTE":
                response.append("Result: We have confirmed this transaction was unauthorized. Your account has been secured.\n")
                        .append("A replacement card will be sent to you within 3-5 business days.\n");
                break;
            case "CLOSE_AS_LOW_RISK":
                response.append("Result: This transaction matches your typical account activity. ")
                        .append("If you have further concerns, please contact us.\n");
                break;
            case "ESCALATE_TO_ANALYST":
                response.append("Result: Your case requires additional review. ")
                        .append("A specialist will contact you within 24 hours.\n");
                break;
            default:
                response.append("Result: Your dispute is under review. We will contact you with an update shortly.\n");
        }

        response.append("\nCase Reference: ").append(caseId).append("\n");
        response.append("Best regards,\nCard Dispute Resolution Team\n");
        return response.toString();
    }
}