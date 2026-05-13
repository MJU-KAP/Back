package com.example.NextPlan.Repository;

import com.example.NextPlan.Entity.UserResume;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface UserResumeRepository extends JpaRepository<UserResume, Integer> {

    List<UserResume> findByUserId(UUID userId);
}
