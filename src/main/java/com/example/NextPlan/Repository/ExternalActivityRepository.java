package com.example.NextPlan.Repository;

import com.example.NextPlan.Entity.ExternalActivity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ExternalActivityRepository extends JpaRepository<ExternalActivity, Integer> {

    Optional<ExternalActivity> findByOriginUrl(String originUrl);

    List<ExternalActivity> findAllByOrderByExtIdDesc();
}
