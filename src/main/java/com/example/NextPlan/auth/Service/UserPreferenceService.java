package com.example.NextPlan.auth.Service;

import com.example.NextPlan.Entity.User;
import com.example.NextPlan.Entity.UserSkill;
import com.example.NextPlan.Repository.UserRepository;
import com.example.NextPlan.Repository.UserSkillRepository;
import com.example.NextPlan.common.CustomException;
import com.example.NextPlan.common.ErrorCode;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserPreferenceService {

    private final UserRepository userRepository;
    private final UserSkillRepository userSkillRepository;

    @Transactional
    public void savePreference(UUID userId, List<String> desiredJobs, List<String> skills) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.FORBIDDEN));

        user.updateDesiredJobs(desiredJobs);

        userSkillRepository.deleteByUser(user);

        List<UserSkill> userSkills = skills.stream()
                .filter(skill -> skill != null && !skill.isBlank())
                .distinct()
                .map(skill -> UserSkill.builder()
                        .user(user)
                        .skillName(skill)
                        .proficiency((short) 0)
                        .build())
                .toList();
        userSkillRepository.saveAll(userSkills);
    }
}
