package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "customer_response_draft")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerResponseDraft {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String caseId;

    @Lob
    @Column(nullable = false)
    private String responseDraft;

    private String status;

    private LocalDateTime createdAt;

    private LocalDateTime approvedAt;

    private String approvedBy;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
