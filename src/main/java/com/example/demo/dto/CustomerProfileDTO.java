package com.example.demo.dto;

import com.example.demo.enums.RiskTier;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerProfileDTO {
    private Long customerId;
    private String customerName;
    private String homeCity;
    private Double averageTransactionAmount;
    private Integer totalTransactions;
    private RiskTier riskTier;
}
