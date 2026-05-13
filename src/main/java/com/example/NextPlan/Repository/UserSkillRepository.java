package com.example.NextPlan.Repository;
import com.example.NextPlan.Entity.User;
import com.example.NextPlan.Entity.UserSkill;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface UserSkillRepository extends JpaRepository<UserSkill, Integer> {

    List<UserSkill> findByUser(User user);
    void deleteByUser(User user);
}
