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
    public void savePreference(UUID userId, String desiredJobRole, List<String> techStacks) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.FORBIDDEN));

        user.updateDesiredJobs(List.of(desiredJobRole.trim()));

        userSkillRepository.deleteByUser(user);

        List<UserSkill> userSkills = techStacks.stream()
                .filter(techStack -> techStack != null && !techStack.isBlank())
                .map(String::trim)
                .distinct()
                .map(techStack -> UserSkill.builder()
                        .user(user)
                        .skillName(techStack)
                        .proficiency((short) 0)
                        .build())
                .toList();
        userSkillRepository.saveAll(userSkills);
    }
}
