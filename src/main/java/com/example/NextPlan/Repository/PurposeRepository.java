package com.example.NextPlan.Repository;

import com.example.NextPlan.Entity.Purpose;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PurposeRepository extends JpaRepository<Purpose, Integer> {

    List<Purpose> findByUserIdOrderByTargetDateAscPurposeIdDesc(UUID userId);
}
