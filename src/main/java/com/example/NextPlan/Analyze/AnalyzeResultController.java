package com.example.NextPlan.Analyze;

import com.example.NextPlan.Analyze.AnalyzeResultService.AnalyzeResultResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
