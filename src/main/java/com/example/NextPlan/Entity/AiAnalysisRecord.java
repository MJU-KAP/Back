package com.example.NextPlan.Entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "ai_analysis_records")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class AiAnalysisRecord {

    @Id
    @UuidGenerator
    @JdbcTypeCode(SqlTypes.UUID)
    @Column(name = "record_id", nullable = false)
    private UUID recordId;

    @JdbcTypeCode(SqlTypes.UUID)
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "analysis_type", nullable = false, length = 100)
    private String analysisType;

    @Column(name = "resume_id")
    private Integer resumeId;

    @Column(name = "input_summary", columnDefinition = "TEXT")
    private String inputSummary;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "result", nullable = false, columnDefinition = "TEXT")
    private String result;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    public void updateResult(String result) {
        this.result = result;
    }

    public void updateStatus(String status) {
        this.status = status;
    }

    public void updateResultAndStatus(String result, String status) {
        this.result = result;
        this.status = status;
    }
}
