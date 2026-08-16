package com.example.demo.repository;

import com.example.demo.entity.PriorDispute;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface PriorDisputeRepository extends JpaRepository<PriorDispute, Long> {
    List<PriorDispute> findByCustomerId(Long customerId);
}
