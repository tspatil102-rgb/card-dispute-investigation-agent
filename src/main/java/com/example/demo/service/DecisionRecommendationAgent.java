package com.example.demo.service;

import com.example.demo.dto.EvidenceBundle;
import com.example.demo.dto.RecommendationDTO;
import com.example.demo.dto.RiskResult;
import com.example.demo.enums.RecommendationDecision;
import com.example.demo.enums.RecommendedAction;
import dev.langchain4j.model.chat.ChatLanguageModel;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;

@Service
public class DecisionRecommendationAgent {
    @Autowired
    private AuditService auditService;

    @Autowired(required = false)
    private ChatLanguageModel chatLanguageModel;

    @Value("${llm.max-output-retries:2}")
    private Integer maxRetries;
    @Value("${llm.recommendation.force:false}")
    private boolean forceLlmRecommendation;
    @Value("${llm.openai.model:gpt-3.5-turbo}")
    private String llmModelName;

    public RecommendationDTO generateRecommendation(String caseId, EvidenceBundle evidence, RiskResult riskResult) {
        RecommendationDTO recommendation = null;
        String modelName = null;

        boolean attemptedLlm = false;
        String auditTool = "RULE_BASED_DECISION";

        // Try LLM-based recommendation if available or forced
        if (chatLanguageModel != null) {
            attemptedLlm = true;
            try {
                recommendation = tryLlmRecommendation(evidence, riskResult);
                if (recommendation != null) {
                    modelName = llmModelName != null ? llmModelName : "gpt-3.5-turbo";
                    auditTool = "LLM_DECISION";
                } else {
                    auditTool = "LLM_DECISION_FAILED";
                }
            } catch (Exception e) {
                auditTool = "LLM_DECISION_FAILED";
                // preserve exception info in audit
                auditService.logStep(caseId, "RECOMMENDATION", "DecisionRecommendationAgent",
                        auditTool,
                        "Risk score: " + riskResult.getRiskScore(),
                        "LLM failure: " + e.getMessage(),
                        llmModelName, "V1", riskResult.getRiskScore());
            }
        } else if (forceLlmRecommendation) {
            // Configuration requested LLM but bean not available
            auditService.logStep(caseId, "RECOMMENDATION", "DecisionRecommendationAgent",
                    "LLM_NOT_AVAILABLE",
                    "Risk score: " + riskResult.getRiskScore(),
                    "LLM bean not initialized; fell back to rule-based decision",
                    null, "V1", riskResult.getRiskScore());
        }

        // Fallback to rule-based decision if LLM didn't produce a recommendation
        if (recommendation == null) {
            recommendation = generateRuleBasedRecommendation(riskResult, evidence);
        }

        // Final audit log: record whether LLM was attempted and whether it succeeded
        String finalTool;
        if (attemptedLlm) {
            finalTool = modelName != null ? "LLM_DECISION" : "LLM_DECISION_FAILED";
        } else {
            finalTool = "RULE_BASED_DECISION";
        }

        auditService.logStep(caseId, "RECOMMENDATION", "DecisionRecommendationAgent",
                finalTool,
                "Risk score: " + riskResult.getRiskScore(),
                "Recommendation: " + recommendation.getDecision(),
                modelName, "V1", riskResult.getRiskScore());

        return recommendation;
    }

    private RecommendationDTO tryLlmRecommendation(EvidenceBundle evidence, RiskResult riskResult) {
        try {
            String prompt = buildRecommendationPrompt(evidence, riskResult);
            String response = chatLanguageModel.generate(prompt);
            return parseRecommendationResponse(response, riskResult);
        } catch (Exception e) {
            System.err.println("LLM recommendation failed: " + e.getMessage());
            return null;
        }
    }

    private String buildRecommendationPrompt(EvidenceBundle evidence, RiskResult riskResult) {
        return """
You are a decision support agent for banking dispute triage.
Use ONLY the provided evidence to generate a recommendation.
Do not invent information.

Risk Assessment:
- Risk Score: %d/100
- Risk Band: %s
- Triggered Rules: %s

Decision options: APPROVE_DISPUTE, CLOSE_AS_LOW_RISK, ESCALATE_TO_ANALYST
Actions: BLOCK_CARD, REISSUE_CARD, TEMPORARY_CREDIT, REQUEST_MORE_INFORMATION, NO_ACTION_REQUIRED

Return ONLY valid JSON format, no markdown:
{"decision": "APPROVE_DISPUTE|CLOSE_AS_LOW_RISK|ESCALATE_TO_ANALYST", "confidence": "HIGH|MEDIUM|LOW", "reason": "explanation", "recommendedActions": ["ACTION1", "ACTION2"]}

Evidence summary: Risk score %d, Band %s, %d rules triggered
Generate recommendation:
""".formatted(
    riskResult.getRiskScore(),
    riskResult.getRiskBand(),
    riskResult.getTriggeredRules().size(),
    riskResult.getRiskScore(),
    riskResult.getRiskBand(),
    riskResult.getTriggeredRules().size()
        );
    }

    private RecommendationDTO parseRecommendationResponse(String jsonResponse, RiskResult riskResult) {
        try {
            Gson gson = new Gson();
            String cleaned = jsonResponse.replaceAll("```json\\s*", "").replaceAll("```\\s*", "");
            JsonObject json = gson.fromJson(cleaned, JsonObject.class);

            RecommendationDTO dto = new RecommendationDTO();
            dto.setRiskScore(riskResult.getRiskScore());
            dto.setRiskBand(riskResult.getRiskBand().toString());

            if (json.has("decision")) {
                String decisionStr = json.get("decision").getAsString();
                dto.setDecision(RecommendationDecision.valueOf(decisionStr));
            }

            if (json.has("confidence")) {
                dto.setConfidence(json.get("confidence").getAsString());
            }

            if (json.has("reason")) {
                dto.setReason(json.get("reason").getAsString());
            }

            List<RecommendedAction> actions = new ArrayList<>();
            if (json.has("recommendedActions")) {
                json.getAsJsonArray("recommendedActions").forEach(elem -> {
                    try {
                        actions.add(RecommendedAction.valueOf(elem.getAsString()));
                    } catch (IllegalArgumentException e) {
                        // Skip invalid actions
                    }
                });
            }
            dto.setRecommendedActions(actions);

            return dto;
        } catch (Exception e) {
            return null;
        }
    }

    private String calculateConfidence(RiskResult riskResult, EvidenceBundle evidence) {
        // HIGH: risk band is HIGH or LOW and all required evidence is present
        if ((riskResult.getRiskBand().toString().equals("HIGH") || riskResult.getRiskBand().toString().equals("LOW")) &&
            allEvidencePresent(evidence)) {
            return "HIGH";
        }

        // MEDIUM: risk band is MEDIUM or one evidence item is missing
        if (riskResult.getRiskBand().toString().equals("MEDIUM") || !allEvidencePresent(evidence)) {
            return "MEDIUM";
        }

        // LOW: transaction match is uncertain
        return "LOW";
    }

    private boolean allEvidencePresent(EvidenceBundle evidence) {
        return evidence.getTransaction() != null &&
               evidence.getCustomerProfile() != null &&
               evidence.getCardStatus() != null;
    }

    private RecommendationDTO generateRuleBasedRecommendation(RiskResult riskResult, EvidenceBundle evidence) {
        RecommendationDTO recommendation = new RecommendationDTO();
        recommendation.setRiskScore(riskResult.getRiskScore());
        recommendation.setRiskBand(riskResult.getRiskBand().toString());

        List<RecommendedAction> actions = new ArrayList<>();

        // Decision logic based on risk band and evidence
        if (riskResult.getRiskBand().toString().equals("HIGH")) {
            recommendation.setDecision(RecommendationDecision.APPROVE_DISPUTE);
            recommendation.setConfidence("HIGH");
            actions.add(RecommendedAction.BLOCK_CARD);
            actions.add(RecommendedAction.REISSUE_CARD);
            recommendation.setReason("Transaction demonstrates high-risk indicators including foreign location, lost card, or unusual amount. Recommend approval of dispute and card replacement.");
        } else if (riskResult.getRiskBand().toString().equals("LOW")) {
            recommendation.setDecision(RecommendationDecision.CLOSE_AS_LOW_RISK);
            recommendation.setConfidence("HIGH");
            actions.add(RecommendedAction.NO_ACTION_REQUIRED);
            recommendation.setReason("Transaction matches customer's typical behavior pattern with no fraud indicators detected. Recommend closure as legitimate transaction.");
        } else {
            recommendation.setDecision(RecommendationDecision.ESCALATE_TO_ANALYST);
            recommendation.setConfidence("MEDIUM");
            actions.add(RecommendedAction.REQUEST_MORE_INFORMATION);
            recommendation.setReason("Transaction presents mixed signals with moderate risk indicators. Recommend escalation to analyst for manual review and additional customer information.");
        }

        recommendation.setRecommendedActions(actions);
        return recommendation;
    }
}
