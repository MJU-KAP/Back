package com.example.NextPlan.Kakao.Service;

import com.example.NextPlan.Entity.AiAnalysisRecord;
import com.example.NextPlan.Entity.User;
import com.example.NextPlan.Entity.UserResume;
import com.example.NextPlan.Kakao.common.CustomException;
import com.example.NextPlan.Kakao.common.ErrorCode;
import com.example.NextPlan.Repository.AiAnalysisRecordRepository;
import com.example.NextPlan.Repository.UserRepository;
import com.example.NextPlan.Repository.UserResumeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor

public class MyPageService {
    private final UserRepository userRepository;
    private final AiAnalysisRecordRepository aiAnalysisRecordRepository;
    private final UserResumeRepository userResumeRepository;

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

        return new MyPageResponse(
                user.getNickname(),
                user.getEmail(),
                user.getCreatedAt(),
                analysisRecords.size(),
                resumes,
                analysisRecords
        );
    }

    public record MyPageResponse(
            String nickname,
            String email,
            OffsetDateTime joinedAt,
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
            String result,
            OffsetDateTime createdAt
    ) {
        public static AnalysisRecordResponse from(AiAnalysisRecord record) {
            return new AnalysisRecordResponse(
                    record.getRecordId(),
                    record.getAnalysisType(),
                    record.getInputSummary(),
                    record.getResult(),
                    record.getCreatedAt()
            );
        }
    }
}
