package com.example.NextPlan.Analyze;

import com.example.NextPlan.Analyze.AnalyzeResultController.AnalyzeResultDetailResponse;
import com.example.NextPlan.Entity.AiAnalysisRecord;
import com.example.NextPlan.Entity.UserResume;
import com.example.NextPlan.Kakao.common.CustomException;
import com.example.NextPlan.Kakao.common.ErrorCode;
import com.example.NextPlan.Repository.AiAnalysisRecordRepository;
import com.example.NextPlan.Repository.UserResumeRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AnalyzeResultService {

    private static final String STATUS_SUCCESS = "SUCCESS";

    private final UserResumeRepository userResumeRepository;
    private final AiAnalysisRecordRepository aiAnalysisRecordRepository;
    private final ObjectMapper objectMapper;

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
                .resumeId(resume.getResumeId())
                .analysisType(analysisType)
                .inputSummary(inputSummary)
                .status(STATUS_SUCCESS)
                .result(result)
                .createdAt(OffsetDateTime.now())
                .build();

        AiAnalysisRecord savedRecord = aiAnalysisRecordRepository.save(record);

        return AnalyzeResultResponse.from(savedRecord, resume, parseResult(savedRecord.getResult()));
    }

    @Transactional(readOnly = true)
    public AnalyzeResultDetailResponse getAnalyzeResult(UUID userId, UUID analysisId) {
        AiAnalysisRecord record = aiAnalysisRecordRepository.findById(analysisId)
                .orElseThrow(() -> new CustomException(ErrorCode.INVALID_REQUEST));

        validateOwner(record, userId);

        UserResume resume = findResume(record);

        return new AnalyzeResultDetailResponse(
                record.getRecordId(),
                record.getResumeId(),
                resume == null ? null : resume.getFileName(),
                record.getAnalysisType(),
                record.getInputSummary(),
                record.getStatus(),
                parseResult(record.getResult()),
                record.getCreatedAt()
        );
    }

    @Transactional
    public void deleteAnalyzeResult(UUID userId, UUID analysisId) {
        AiAnalysisRecord record = aiAnalysisRecordRepository.findById(analysisId)
                .orElseThrow(() -> new CustomException(ErrorCode.INVALID_REQUEST));

        validateOwner(record, userId);

        aiAnalysisRecordRepository.delete(record);
    }

    private void validateOwner(AiAnalysisRecord record, UUID userId) {
        if (!record.getUserId().equals(userId)) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }
    }

    private UserResume findResume(AiAnalysisRecord record) {
        Integer resumeId = record.getResumeId();

        if (resumeId == null) {
            return null;
        }

        return userResumeRepository.findById(resumeId)
                .orElse(null);
    }

    private JsonNode parseResult(String result) {
        if (!StringUtils.hasText(result)) {
            return objectMapper.createObjectNode();
        }

        try {
            return objectMapper.readTree(result);
        } catch (JsonProcessingException e) {
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    public record AnalyzeResultResponse(
            UUID recordId,
            Integer resumeId,
            UUID userId,
            String fileName,
            String analysisType,
            String inputSummary,
            String status,
            JsonNode result,
            OffsetDateTime createdAt
    ) {
        public static AnalyzeResultResponse from(AiAnalysisRecord record, UserResume resume, JsonNode result) {
            return new AnalyzeResultResponse(
                    record.getRecordId(),
                    resume.getResumeId(),
                    record.getUserId(),
                    resume.getFileName(),
                    record.getAnalysisType(),
                    record.getInputSummary(),
                    record.getStatus(),
                    result,
                    record.getCreatedAt()
            );
        }
    }
}
