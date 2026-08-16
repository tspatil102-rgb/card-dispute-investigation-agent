package com.example.demo.entity;

import com.example.demo.enums.MerchantCategory;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "card_transaction")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CardTransaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String transactionId;

    @Column(nullable = false)
    private Long customerId;

    @Column(nullable = false)
    private Double amount;

    @Column(nullable = false)
    private String merchant;

    @Column(nullable = false)
    private String city;

    private String deviceId;

    @Enumerated(EnumType.STRING)
    private MerchantCategory merchantCategory;

    private LocalDateTime transactionDate;
}
