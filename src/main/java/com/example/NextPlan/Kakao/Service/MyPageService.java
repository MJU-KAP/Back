package com.example.NextPlan.Kakao.Service;

import com.example.NextPlan.Entity.AiAnalysisRecord;
import com.example.NextPlan.Entity.User;
import com.example.NextPlan.Entity.UserResume;
import com.example.NextPlan.Entity.UserSkill;
import com.example.NextPlan.Kakao.common.CustomException;
import com.example.NextPlan.Kakao.common.ErrorCode;
import com.example.NextPlan.Repository.AiAnalysisRecordRepository;
import com.example.NextPlan.Repository.UserRepository;
import com.example.NextPlan.Repository.UserResumeRepository;
import com.example.NextPlan.Repository.UserSkillRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor

public class MyPageService {
    private final UserRepository userRepository;
    private final AiAnalysisRecordRepository aiAnalysisRecordRepository;
    private final UserResumeRepository userResumeRepository;
    private final UserSkillRepository userSkillRepository;

    @Transactional(readOnly = true)
    public MyPageResponse getMyPage(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.FORBIDDEN));

        List<AiAnalysisRecord> records =
                aiAnalysisRecordRepository.findByUserIdOrderByCreatedAtDesc(userId);

        List<AnalysisRecordResponse> analysisRecords = records.stream()
                .map(AnalysisRecordResponse::from)
                .toList();

        List<ResumeResponse> resumes = userResumeRepository.findByUserId(userId)
                .stream()
                .map(ResumeResponse::from)
                .toList();

        String desiredJobRole = getDesiredJobRole(user);
        List<String> techStacks = getTechStacks(user);

        return new MyPageResponse(
                user.getNickname(),
                user.getEmail(),
                user.getCreatedAt(),
                desiredJobRole,
                techStacks,
                analysisRecords.size(),
                resumes,
                analysisRecords
        );
    }

    private String getDesiredJobRole(User user) {
        String[] desiredJobs = user.getDesiredJobs();

        if (desiredJobs == null) {
            return null;
        }

        return Arrays.stream(desiredJobs)
                .filter(StringUtils::hasText)
                .findFirst()
                .orElse(null);
    }

    private List<String> getTechStacks(User user) {
        return userSkillRepository.findByUser(user)
                .stream()
                .map(UserSkill::getSkillName)
                .filter(skillNames -> skillNames != null)
                .flatMap(Arrays::stream)
                .filter(StringUtils::hasText)
                .distinct()
                .toList();
    }

    public record MyPageResponse(
            String nickname,
            String email,
            OffsetDateTime joinedAt,
            String desiredJobRole,
            List<String> techStacks,
            int analysisCount,
            List<ResumeResponse> resumes,
            List<AnalysisRecordResponse> analysisRecords
    ) {
    }

    public record ResumeResponse(
            Integer resumeId,
            String fileName,
            String fileUrl
    ) {
        public static ResumeResponse from(UserResume resume) {
            return new ResumeResponse(
                    resume.getResumeId(),
                    resume.getFileName(),
                    resume.getFileUrl()
            );
        }
    }

    public record AnalysisRecordResponse(
            UUID recordId,
            String analysisType,
            String inputSummary,
            OffsetDateTime createdAt
    ) {
        public static AnalysisRecordResponse from(AiAnalysisRecord record) {
            return new AnalysisRecordResponse(
                    record.getRecordId(),
                    record.getAnalysisType(),
                    record.getInputSummary(),
                    record.getCreatedAt()
            );
        }
    }
}
