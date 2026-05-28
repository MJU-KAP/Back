package com.example.NextPlan.Analyze;

import com.example.NextPlan.Entity.AiAnalysisRecord;

import java.time.OffsetDateTime;
import java.util.UUID;

public record AnalysisRecordResponse(
        UUID recordId,
        Integer resumeId,
        String analysisType,
        String inputSummary,
        String status,
        String result,
        OffsetDateTime createdAt
) {
    public static AnalysisRecordResponse from(AiAnalysisRecord record) {
        return new AnalysisRecordResponse(
                record.getRecordId(),
                record.getResumeId(),
                record.getAnalysisType(),
                record.getInputSummary(),
                record.getStatus(),
                record.getResult(),
                record.getCreatedAt()
        );
    }
}
