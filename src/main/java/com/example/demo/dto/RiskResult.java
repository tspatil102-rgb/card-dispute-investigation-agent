package com.example.demo.dto;

import com.example.demo.enums.RiskBand;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RiskResult {
    private Integer riskScore;
    private RiskBand riskBand;
    private List<RiskRule> triggeredRules;
}
