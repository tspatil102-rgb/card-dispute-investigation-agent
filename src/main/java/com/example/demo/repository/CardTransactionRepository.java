package com.example.demo.repository;

import com.example.demo.entity.CardTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface CardTransactionRepository extends JpaRepository<CardTransaction, Long> {
    Optional<CardTransaction> findByTransactionId(String transactionId);
}
