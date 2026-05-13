package com.example.NextPlan.Repository;

import com.example.NextPlan.Entity.UserActivity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface UserActivityRepository extends JpaRepository<UserActivity, Integer> {

    List<UserActivity> findByUserId(UUID userId);
}
