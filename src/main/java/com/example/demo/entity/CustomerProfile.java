package com.example.demo.entity;

import com.example.demo.enums.RiskTier;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "customer_profile")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerProfile {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private Long customerId;

    @Column(nullable = false)
    private String customerName;

    @Column(nullable = false)
    private String homeCity;

    private Double averageTransactionAmount;

    private Integer totalTransactions;

    @Enumerated(EnumType.STRING)
    private RiskTier riskTier;
}
