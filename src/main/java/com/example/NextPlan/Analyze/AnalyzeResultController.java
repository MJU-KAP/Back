package com.example.NextPlan.Analyze;

import com.example.NextPlan.Analyze.AnalyzeResultService.AnalyzeResultResponse;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/analyze/result")
@RequiredArgsConstructor
public class AnalyzeResultController {

    private final AnalyzeResultService analyzeResultService;

    @PostMapping
    public ResponseEntity<AnalyzeResultResponse> receiveAnalyzeResult(
            @Valid @RequestBody AnalyzeResultRequest request
    ) {
        AnalyzeResultResponse response = analyzeResultService.saveAnalyzeResult(
                request.resumeId(),
                request.analysisType(),
                request.inputSummary(),
                request.result()
        );

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{analysisId}")
    public ResponseEntity<AnalyzeResultDetailResponse> getAnalyzeResult(
            Authentication authentication,
            @PathVariable UUID analysisId
    ) {
        UUID userId = UUID.fromString(authentication.getName());

        AnalyzeResultDetailResponse response = analyzeResultService.getAnalyzeResult(userId, analysisId);

        return ResponseEntity.ok(response);
    }

    public record AnalyzeResultRequest(
            @NotNull(message = "resumeId is required")
            Integer resumeId,

            @NotBlank(message = "analysisType is required")
            String analysisType,

            String inputSummary,

            @NotBlank(message = "result is required")
            String result
    ) {
    }

    public record AnalyzeResultDetailResponse(
            UUID recordId,
            String analysisType,
            String inputSummary,
            JsonNode result,
            java.time.OffsetDateTime createdAt
    ) {
    }
}
