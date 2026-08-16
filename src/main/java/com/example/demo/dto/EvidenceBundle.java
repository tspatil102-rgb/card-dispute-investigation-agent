package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EvidenceBundle {
    private String caseId;
    private ComplaintExtractDTO complaintExtract;
    private TransactionDTO transaction;
    private CustomerProfileDTO customerProfile;
    private CardStatusDTO cardStatus;
    private List<PriorDisputeDTO> priorDisputes;
}
