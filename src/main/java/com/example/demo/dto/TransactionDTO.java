package com.example.demo.dto;

import com.example.demo.enums.MerchantCategory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionDTO {
    private String transactionId;
    private Double amount;
    private String merchant;
    private String city;
    private String deviceId;
    private MerchantCategory merchantCategory;
    private LocalDateTime transactionDate;
}
