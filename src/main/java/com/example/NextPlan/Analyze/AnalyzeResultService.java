package com.example.NextPlan.Analyze;

import com.example.NextPlan.Entity.AiAnalysisRecord;
import com.example.NextPlan.Entity.UserResume;
import com.example.NextPlan.Kakao.common.CustomException;
import com.example.NextPlan.Kakao.common.ErrorCode;
import com.example.NextPlan.Repository.AiAnalysisRecordRepository;
import com.example.NextPlan.Repository.UserResumeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AnalyzeResultService {

    private final UserResumeRepository userResumeRepository;
    private final AiAnalysisRecordRepository aiAnalysisRecordRepository;

    @Transactional
    public AnalyzeResultResponse saveAnalyzeResult(
            Integer resumeId,
            String analysisType,
            String inputSummary,
            String result
    ) {
        UserResume resume = userResumeRepository.findById(resumeId)
                .orElseThrow(() -> new CustomException(ErrorCode.INVALID_REQUEST));

        AiAnalysisRecord record = AiAnalysisRecord.builder()
                .userId(resume.getUserId())
                .analysisType(analysisType)
                .inputSummary(inputSummary)
                .result(result)
                .createdAt(OffsetDateTime.now())
                .build();

        AiAnalysisRecord savedRecord = aiAnalysisRecordRepository.save(record);

        return AnalyzeResultResponse.from(savedRecord, resume);
    }

    public record AnalyzeResultResponse(
            UUID recordId,
            Integer resumeId,
            UUID userId,
            String fileName,
            String analysisType,
            String inputSummary,
            String result,
            OffsetDateTime createdAt
    ) {
        public static AnalyzeResultResponse from(AiAnalysisRecord record, UserResume resume) {
            return new AnalyzeResultResponse(
                    record.getRecordId(),
                    resume.getResumeId(),
                    record.getUserId(),
                    resume.getFileName(),
                    record.getAnalysisType(),
                    record.getInputSummary(),
                    record.getResult(),
                    record.getCreatedAt()
            );
        }
    }
}
