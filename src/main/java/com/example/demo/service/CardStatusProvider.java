package com.example.demo.service;

import com.example.demo.dto.CardStatusDTO;
import com.example.demo.enums.CardStatus;
import org.springframework.stereotype.Service;

@Service
public class CardStatusProvider {
    
    public CardStatusDTO getCardStatus(Long customerId) {
        // Mock card status lookup based on customer ID
        // In production, this would query actual card system
        
        switch (customerId.intValue()) {
            case 1001:
                // Scenario 1: Card is LOST
                return new CardStatusDTO(CardStatus.LOST);
            case 1002:
                // Scenario 2: Card is ACTIVE
                return new CardStatusDTO(CardStatus.ACTIVE);
            case 1003:
                // Scenario 3: Card is ACTIVE
                return new CardStatusDTO(CardStatus.ACTIVE);
            default:
                return new CardStatusDTO(CardStatus.ACTIVE);
        }
    }
}
