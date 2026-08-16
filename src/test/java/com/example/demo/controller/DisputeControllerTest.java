package com.example.demo.controller;

import com.example.demo.dto.CreateDisputeRequest;
import com.example.demo.enums.DisputeStatus;
import com.example.demo.service.DisputeService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.notNullValue;

@SpringBootTest
@AutoConfigureMockMvc
public class DisputeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private DisputeService disputeService;

    @Test
    public void testCreateDispute() throws Exception {
        CreateDisputeRequest request = new CreateDisputeRequest();
        request.setCustomerId(1001L);
        request.setComplaintText("I did not make this transaction");

        MvcResult result = mockMvc.perform(post("/api/disputes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.caseId", notNullValue()))
                .andExpect(jsonPath("$.status").value(DisputeStatus.NEW.toString()))
                .andReturn();

        System.out.println("Create Dispute Response: " + result.getResponse().getContentAsString());
    }

    @Test
    public void testGetDispute() throws Exception {
        // First create a dispute
        CreateDisputeRequest request = new CreateDisputeRequest();
        request.setCustomerId(1002L);
        request.setComplaintText("Suspicious transaction");

        MvcResult createResult = mockMvc.perform(post("/api/disputes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        String responseBody = createResult.getResponse().getContentAsString();
        String caseId = objectMapper.readTree(responseBody).get("caseId").asText();

        // Now retrieve it
        mockMvc.perform(get("/api/disputes/{caseId}", caseId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.caseId").value(caseId));
    }

    @Test
    public void testInvestigateDispute() throws Exception {
        // Create a dispute
        CreateDisputeRequest request = new CreateDisputeRequest();
        request.setCustomerId(1001L);
        request.setComplaintText("Unauthorized transaction of ₹5000");

        MvcResult createResult = mockMvc.perform(post("/api/disputes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        String caseId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .get("caseId").asText();

        // Investigate it
        mockMvc.perform(post("/api/disputes/{caseId}/investigate", caseId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.caseId").value(caseId))
                .andExpect(jsonPath("$.status").value(DisputeStatus.PENDING_ANALYST_REVIEW.toString()))
                .andExpect(jsonPath("$.riskResult.riskScore", notNullValue()))
                .andExpect(jsonPath("$.recommendation.decision", notNullValue()));
    }

    @Test
    public void testGetAuditTrail() throws Exception {
        // Create and investigate
        CreateDisputeRequest request = new CreateDisputeRequest();
        request.setCustomerId(1002L);
        request.setComplaintText("Test complaint");

        MvcResult createResult = mockMvc.perform(post("/api/disputes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        String caseId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .get("caseId").asText();

        disputeService.investigateDispute(caseId);

        // Get audit trail
        mockMvc.perform(get("/api/disputes/{caseId}/audit", caseId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    public void testGetTimeline() throws Exception {
        CreateDisputeRequest request = new CreateDisputeRequest();
        request.setCustomerId(1003L);
        request.setComplaintText("Timeline test");

        MvcResult createResult = mockMvc.perform(post("/api/disputes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        String caseId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .get("caseId").asText();

        disputeService.investigateDispute(caseId);

        mockMvc.perform(get("/api/disputes/{caseId}/timeline", caseId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    public void testGetCustomerResponseDraft() throws Exception {
        CreateDisputeRequest request = new CreateDisputeRequest();
        request.setCustomerId(1001L);
        request.setComplaintText("Response draft test");

        MvcResult createResult = mockMvc.perform(post("/api/disputes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        String caseId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .get("caseId").asText();

        disputeService.investigateDispute(caseId);

        mockMvc.perform(get("/api/disputes/{caseId}/customer-response", caseId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.caseId").value(caseId))
                .andExpect(jsonPath("$.responseDraft", notNullValue()));
    }

    @Test
    public void testNonexistentCase() throws Exception {
        mockMvc.perform(get("/api/disputes/NONEXISTENT"))
                .andExpect(status().isNotFound());
    }
}
