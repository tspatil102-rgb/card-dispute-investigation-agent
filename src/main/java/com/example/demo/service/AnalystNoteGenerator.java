package com.example.demo.service;

import com.example.demo.dto.EvidenceBundle;
import com.example.demo.dto.RecommendationDTO;
import com.example.demo.dto.RiskResult;
import dev.langchain4j.model.chat.ChatLanguageModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AnalystNoteGenerator {
    @Autowired
    private AuditService auditService;

    @Autowired(required = false)
    private ChatLanguageModel chatLanguageModel;

    public String generateAnalystNote(String caseId, EvidenceBundle evidence, RiskResult riskResult,
                                      RecommendationDTO recommendation) {
        String note = null;
        String modelName = null;

        // Try LLM-based generation if available
        if (chatLanguageModel != null) {
            note = tryLlmAnalystNote(evidence, riskResult, recommendation);
            if (note != null) {
                modelName = "gpt-3.5-turbo";
            }
        }

        // Fallback to template-based generation
        if (note == null) {
            note = generateTemplateAnalystNote(evidence, riskResult, recommendation);
        }

        // Audit log
        auditService.logStep(caseId, "ANALYST_NOTE", "AnalystNoteGenerator",
                modelName != null ? "LLM_GENERATION" : "TEMPLATE_GENERATION",
                "Generated note for case",
                "Analyst note created",
                modelName, "V1", riskResult.getRiskScore());

        return note;
    }

    private String tryLlmAnalystNote(EvidenceBundle evidence, RiskResult riskResult, RecommendationDTO recommendation) {
        try {
            String prompt = buildAnalystNotePrompt(evidence, riskResult, recommendation);
            return chatLanguageModel.generate(prompt);
        } catch (Exception e) {
            System.err.println("LLM analyst note generation failed: " + e.getMessage());
            return null;
        }
    }

    private String buildAnalystNotePrompt(EvidenceBundle evidence, RiskResult riskResult, RecommendationDTO recommendation) {
        StringBuilder evidenceSummary = new StringBuilder();
        
        if (evidence.getComplaintExtract() != null) {
            evidenceSummary.append("Complaint: Amount ₹").append(evidence.getComplaintExtract().getAmount())
                    .append(" at ").append(evidence.getComplaintExtract().getMerchant()).append("\n");
        }
        
        if (evidence.getTransaction() != null) {
            evidenceSummary.append("Transaction verified in ").append(evidence.getTransaction().getCity()).append("\n");
        }
        
        if (evidence.getCustomerProfile() != null) {
            evidenceSummary.append("Customer: ").append(evidence.getCustomerProfile().getCustomerName())
                    .append(" from ").append(evidence.getCustomerProfile().getHomeCity())
                    .append(" (Avg spend: ₹").append(evidence.getCustomerProfile().getAverageTransactionAmount()).append(")\n");
        }

return String.format("""
You are a banking dispute analyst note writer.
Write a concise, professional internal note summarizing the case, findings, and recommendation.
Target length: 200-300 words.
Be factual and objective.

Risk Score: %%d/100 (Band: %%s)
Recommendation: %%s (Confidence: %%s)

Evidence:
%%s

Write the analyst note:
""",
            riskResult.getRiskScore(),
            riskResult.getRiskBand(),
            recommendation.getDecision(),
            recommendation.getConfidence(),
            evidenceSummary.toString()
        );
    }

    private String generateTemplateAnalystNote(EvidenceBundle evidence, RiskResult riskResult, RecommendationDTO recommendation) {
        StringBuilder note = new StringBuilder();
        note.append("=== ANALYST CASE SUMMARY ===\n");
        note.append("Complaint: ₹").append(evidence.getComplaintExtract().getAmount()).append(" at ")
                .append(evidence.getComplaintExtract().getMerchant()).append("\n");
        note.append("Risk Score: ").append(riskResult.getRiskScore()).append("/100 (").append(riskResult.getRiskBand()).append(")\n");
        note.append("Recommendation: ").append(recommendation.getDecision()).append(" - ").append(recommendation.getReason()).append("\n");
        note.append("=== END OF ANALYST NOTE ===\n");
        return note.toString();
    }
}
