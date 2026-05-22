package com.example.NextPlan.Analyze;

import com.example.NextPlan.Entity.AiAnalysisRecord;

import java.time.OffsetDateTime;
import java.util.UUID;

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
