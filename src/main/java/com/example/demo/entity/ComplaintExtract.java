package com.example.demo.entity;

import com.example.demo.enums.ComplaintType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "complaint_extract")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ComplaintExtract {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String caseId;

    private Double amount;

    private String merchant;

    private String transactionDateText;

    @Enumerated(EnumType.STRING)
    private ComplaintType complaintType;

    private String additionalDetails;
}
