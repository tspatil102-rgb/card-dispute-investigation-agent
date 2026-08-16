package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "prior_dispute")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PriorDispute {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long customerId;

    @Column(nullable = false)
    private String priorCaseId;

    @Column(nullable = false)
    private String status;

    private String reason;
}
