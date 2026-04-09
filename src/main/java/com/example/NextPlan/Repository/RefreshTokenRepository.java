package com.example.NextPlan.Repository;

import com.example.NextPlan.Entity.RefreshToken;
import com.example.NextPlan.Entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByUser(User user);

    void deleteByUser(User user);
}
