package com.example.demo.service;

import com.example.demo.dto.*;
import com.example.demo.entity.CardTransaction;
import com.example.demo.entity.CustomerProfile;
import com.example.demo.entity.PriorDispute;
import com.example.demo.repository.CardTransactionRepository;
import com.example.demo.repository.CustomerProfileRepository;
import com.example.demo.repository.PriorDisputeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class InvestigationAgent {
    @Autowired
    private CardTransactionRepository transactionRepository;

    @Autowired
    private CustomerProfileRepository customerProfileRepository;

    @Autowired
    private PriorDisputeRepository priorDisputeRepository;

    @Autowired
    private CardStatusProvider cardStatusProvider;

    @Autowired
    private AuditService auditService;

    public EvidenceBundle investigateDispute(String caseId, ComplaintExtractDTO complaintExtract,
                                            Long customerId) {
        EvidenceBundle bundle = new EvidenceBundle();
        bundle.setCaseId(caseId);
        bundle.setComplaintExtract(complaintExtract);

        // Tool: Find matching transaction
        TransactionDTO transaction = findMatchingTransaction(complaintExtract, customerId);
        bundle.setTransaction(transaction);

        // Tool: Get customer profile
        CustomerProfileDTO customerProfile = getCustomerProfile(customerId);
        bundle.setCustomerProfile(customerProfile);

        // Tool: Check card status
        CardStatusDTO cardStatus = cardStatusProvider.getCardStatus(customerId);
        bundle.setCardStatus(cardStatus);

        // Tool: Get prior disputes
        List<PriorDisputeDTO> priorDisputes = getPriorDisputes(customerId);
        bundle.setPriorDisputes(priorDisputes);

        // Audit log
        auditService.logStep(caseId, "INVESTIGATION", "InvestigationAgent", "TRANSACTION_LOOKUP",
                "Transaction amount: " + complaintExtract.getAmount(),
                "Found transaction: " + (transaction != null ? transaction.getTransactionId() : "NONE"),
                null, "V1", null);

        return bundle;
    }

    private TransactionDTO findMatchingTransaction(ComplaintExtractDTO complaint, Long customerId) {
        // Mock implementation: find transaction by amount and merchant
        // In production, would query actual transaction database
        List<CardTransaction> transactions = transactionRepository.findAll();

        for (CardTransaction txn : transactions) {
            if (txn.getCustomerId().equals(customerId) &&
                txn.getAmount().equals(complaint.getAmount()) &&
                txn.getMerchant().equalsIgnoreCase(complaint.getMerchant())) {
                return TransactionDTO.builder()
                        .transactionId(txn.getTransactionId())
                        .amount(txn.getAmount())
                        .merchant(txn.getMerchant())
                        .city(txn.getCity())
                        .deviceId(txn.getDeviceId())
                        .merchantCategory(txn.getMerchantCategory())
                        .transactionDate(txn.getTransactionDate())
                        .build();
            }
        }

        return null; // No matching transaction found
    }

    private CustomerProfileDTO getCustomerProfile(Long customerId) {
        Optional<CustomerProfile> profile = customerProfileRepository.findByCustomerId(customerId);
        if (profile.isPresent()) {
            CustomerProfile p = profile.get();
            return CustomerProfileDTO.builder()
                    .customerId(p.getCustomerId())
                    .customerName(p.getCustomerName())
                    .homeCity(p.getHomeCity())
                    .averageTransactionAmount(p.getAverageTransactionAmount())
                    .totalTransactions(p.getTotalTransactions())
                    .riskTier(p.getRiskTier())
                    .build();
        }
        return null;
    }

    private List<PriorDisputeDTO> getPriorDisputes(Long customerId) {
        List<PriorDispute> disputes = priorDisputeRepository.findByCustomerId(customerId);
        List<PriorDisputeDTO> dtos = new ArrayList<>();

        for (PriorDispute dispute : disputes) {
            dtos.add(PriorDisputeDTO.builder()
                    .caseId(dispute.getPriorCaseId())
                    .status(dispute.getStatus())
                    .reason(dispute.getReason())
                    .build());
        }

        return dtos;
    }
}

