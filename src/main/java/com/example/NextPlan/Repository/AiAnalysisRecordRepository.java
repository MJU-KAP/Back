package com.example.NextPlan.Repository;

import com.example.NextPlan.Entity.AiAnalysisRecord;
import com.example.NextPlan.Entity.User;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AiAnalysisRecordRepository extends JpaRepository<AiAnalysisRecord, Long>{
    List<AiAnalysisRecord> findByUserOrderByCreatedAtDesc(UUID userId);
    long countByUser(UUID userId);
}
