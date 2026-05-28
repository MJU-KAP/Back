package com.example.NextPlan.Kakao.Service;

import com.example.NextPlan.Entity.AiAnalysisRecord;
import com.example.NextPlan.Repository.AiAnalysisRecordRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiAnalysisAsyncService {

    private static final String STATUS_SUCCESS = "SUCCESS";
    private static final String STATUS_FAILED = "FAILED";

    private final AiAnalysisRecordRepository aiAnalysisRecordRepository;
    private final WebClient.Builder webClientBuilder;
    private final ObjectMapper objectMapper;

    @Value("${ai.server-url:}")
    private String aiServerUrl;

    @Async
    @Transactional
    public void requestAndSaveAnalysis(
            UUID analysisId,
            String authorizationHeader,
            String desiredJobRole,
            List<String> fileUrls
    ) {
        AiAnalysisRecord analysisRecord = aiAnalysisRecordRepository.findById(analysisId)
                .orElse(null);

        if (analysisRecord == null) {
            log.warn("AI analysis record not found. analysisId={}", analysisId);
            return;
        }

        AiAnalysisResult result = requestAiAnalysis(authorizationHeader, analysisId, desiredJobRole, fileUrls);
        analysisRecord.updateResultAndStatus(result.responseBody(), result.status());
    }

    private AiAnalysisResult requestAiAnalysis(
            String authorizationHeader,
            UUID analysisId,
            String desiredJobRole,
            List<String> fileUrls
    ) {
        if (!StringUtils.hasText(aiServerUrl)) {
            log.warn("AI server request skipped. ai.server-url is empty. analysisId={}", analysisId);
            return AiAnalysisResult.failed(createErrorResult(
                    "AI_SERVER_URL_EMPTY",
                    "AI analysis server URL is not configured.",
                    null
            ));
        }

        log.info(
                "Requesting AI analysis asynchronously. analysisId={}, desiredJobRole={}, fileCount={}, aiServerUrl={}",
                analysisId,
                desiredJobRole,
                fileUrls.size(),
                aiServerUrl
        );

        AiAnalysisRequest request = new AiAnalysisRequest(desiredJobRole, fileUrls);

        try {
            String responseBody = webClientBuilder.build()
                    .post()
                    .uri(aiServerUrl)
                    .header(HttpHeaders.AUTHORIZATION, authorizationHeader)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofMinutes(3))
                    .block();

            log.info("AI analysis request completed. analysisId={}, response={}", analysisId, responseBody);

            return normalizeSuccessResultBody(responseBody);
        } catch (WebClientResponseException e) {
            String responseBody = normalizeErrorResultBody(e.getResponseBodyAsString());
            log.warn(
                    "AI server returned error. analysisId={}, status={}, responseBody={}",
                    analysisId,
                    e.getStatusCode(),
                    responseBody
            );

            return AiAnalysisResult.failed(responseBody);
        } catch (RuntimeException e) {
            log.warn("AI server request failed. analysisId={}", analysisId, e);
            return AiAnalysisResult.failed(createErrorResult(
                    "AI_SERVER_CONNECTION_FAILED",
                    "AI analysis server request failed.",
                    null
            ));
        }
    }

    private AiAnalysisResult normalizeSuccessResultBody(String responseBody) {
        if (!StringUtils.hasText(responseBody)) {
            return AiAnalysisResult.failed(createErrorResult(
                    "AI_SERVER_EMPTY_RESPONSE",
                    "AI analysis server returned an empty response.",
                    null
            ));
        }

        String trimmedBody = responseBody.trim();
        if (trimmedBody.startsWith("{") || trimmedBody.startsWith("[")) {
            return AiAnalysisResult.success(trimmedBody);
        }

        return AiAnalysisResult.failed(createErrorResult(
                "AI_SERVER_NON_JSON_RESPONSE",
                "AI analysis server returned a non-JSON response.",
                trimmedBody
        ));
    }

    private String normalizeErrorResultBody(String responseBody) {
        if (StringUtils.hasText(responseBody)) {
            String trimmedBody = responseBody.trim();
            if (trimmedBody.startsWith("{") || trimmedBody.startsWith("[")) {
                return trimmedBody;
            }

            return createErrorResult(
                    "AI_SERVER_NON_JSON_RESPONSE",
                    "AI analysis server returned a non-JSON response.",
                    trimmedBody
            );
        }

        return createErrorResult(
                "AI_SERVER_ERROR",
                "AI analysis server returned an error.",
                null
        );
    }

    private String createErrorResult(String code, String message, String rawResponse) {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("status", "error");
        root.put("code", code);
        root.put("message", message);

        if (StringUtils.hasText(rawResponse)) {
            root.put("rawResponse", rawResponse);
        }

        try {
            return objectMapper.writeValueAsString(root);
        } catch (JsonProcessingException e) {
            return "{\"status\":\"error\",\"code\":\"AI_SERVER_ERROR\",\"message\":\"AI analysis failed.\"}";
        }
    }

    private record AiAnalysisRequest(
            String desiredJobRole,
            List<String> fileUrls
    ) {
    }

    private record AiAnalysisResult(
            String responseBody,
            String status
    ) {
        private static AiAnalysisResult success(String responseBody) {
            return new AiAnalysisResult(responseBody, STATUS_SUCCESS);
        }

        private static AiAnalysisResult failed(String responseBody) {
            return new AiAnalysisResult(responseBody, STATUS_FAILED);
        }
    }
}
