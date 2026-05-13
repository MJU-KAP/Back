package com.example.NextPlan.Repository;

import com.example.NextPlan.Entity.AiAnalysisRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AiAnalysisRecordRepository extends JpaRepository<AiAnalysisRecord, UUID> {
    List<AiAnalysisRecord> findByUserIdOrderByCreatedAtDesc(UUID userId);
    long countByUserId(UUID userId);
}
