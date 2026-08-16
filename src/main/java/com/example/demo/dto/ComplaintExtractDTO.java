package com.example.demo.dto;

import com.example.demo.enums.ComplaintType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ComplaintExtractDTO {
    private Double amount;
    private String merchant;
    private String transactionDateText;
    private ComplaintType complaintType;
    private String additionalDetails;
}
