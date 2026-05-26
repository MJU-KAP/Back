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
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
public class MyPageService {

    private static final String ANALYSIS_SUFFIX = "\uBD84\uC11D";

    private final UserRepository userRepository;
    private final AiAnalysisRecordRepository aiAnalysisRecordRepository;
    private final UserResumeRepository userResumeRepository;
    private final UserSkillRepository userSkillRepository;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public MyPageResponse getMyPage(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.FORBIDDEN));

        List<AiAnalysisRecord> records =
                aiAnalysisRecordRepository.findByUserIdOrderByCreatedAtDesc(userId);

        List<ResumeResponse> resumes = userResumeRepository.findByUserId(userId)
                .stream()
                .map(ResumeResponse::from)
                .toList();

        List<String> fallbackFileNames = resumes.stream()
                .sorted(Comparator.comparing(ResumeResponse::resumeId).reversed())
                .map(ResumeResponse::fileName)
                .filter(StringUtils::hasText)
                .toList();

        List<AnalysisRecordResponse> analysisRecords = IntStream.range(0, records.size())
                .mapToObj(index -> createAnalysisRecordResponse(
                        records.get(index),
                        getFallbackFileName(fallbackFileNames, index)
                ))
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

    private AnalysisRecordResponse createAnalysisRecordResponse(
            AiAnalysisRecord record,
            String fallbackFileName
    ) {
        String fileName = getRepresentativeFileName(record.getInputSummary(), fallbackFileName);

        return new AnalysisRecordResponse(
                record.getRecordId(),
                record.getAnalysisType(),
                getDisplayInputSummary(record.getInputSummary(), fileName),
                fileName,
                extractTags(record.getResult()),
                record.getCreatedAt()
        );
    }

    private String getFallbackFileName(List<String> fileNames, int index) {
        if (index >= fileNames.size()) {
            return null;
        }

        return fileNames.get(index);
    }

    private String getDisplayInputSummary(String inputSummary, String fileName) {
        if (StringUtils.hasText(inputSummary) && isAnalysisLabel(inputSummary)) {
            return inputSummary;
        }

        if (StringUtils.hasText(fileName)) {
            return fileName;
        }

        return inputSummary;
    }

    private String getRepresentativeFileName(String inputSummary, String fallbackFileName) {
        if (!StringUtils.hasText(inputSummary) || isAnalysisLabel(inputSummary)) {
            return fallbackFileName;
        }

        return Arrays.stream(inputSummary.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .findFirst()
                .orElse(fallbackFileName);
    }

    private boolean isAnalysisLabel(String inputSummary) {
        return inputSummary.endsWith(" " + ANALYSIS_SUFFIX);
    }

    private List<String> extractTags(String result) {
        if (!StringUtils.hasText(result)) {
            return List.of();
        }

        try {
            JsonNode root = objectMapper.readTree(result);
            Set<String> tags = new LinkedHashSet<>();
            collectNameTags(root.path("user_skills"), tags);
            collectNameTags(root.path("skill_gaps"), tags);
            return new ArrayList<>(tags).stream()
                    .limit(3)
                    .toList();
        } catch (JsonProcessingException e) {
            return List.of();
        }
    }

    private void collectNameTags(JsonNode nodes, Set<String> tags) {
        if (!nodes.isArray()) {
            return;
        }

        nodes.forEach(node -> {
            String name = node.path("name").asText("");
            if (StringUtils.hasText(name)) {
                tags.add(name);
            }
        });
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
            String fileName,
            List<String> tags,
            OffsetDateTime createdAt
    ) {
    }
}
