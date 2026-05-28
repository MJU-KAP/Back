package com.example.NextPlan.Kakao.Service;

import com.example.NextPlan.Entity.AiAnalysisRecord;
import com.example.NextPlan.Entity.User;
import com.example.NextPlan.Entity.UserResume;
import com.example.NextPlan.Kakao.common.CustomException;
import com.example.NextPlan.Kakao.common.ErrorCode;
import com.example.NextPlan.Kakao.controller.ReturnResumeController.ResumeListResponse;
import com.example.NextPlan.Kakao.controller.ReturnResumeController.ResumeResponse;
import com.example.NextPlan.Repository.AiAnalysisRecordRepository;
import com.example.NextPlan.Repository.UserRepository;
import com.example.NextPlan.Repository.UserResumeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.io.IOException;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class SaveFileService {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            "pdf", "docx", "txt", "png", "jpg", "jpeg"
    );
    private static final String PROCESSING_RESULT = "{\"status\":\"processing\"}";
    private static final String STATUS_PROCESSING = "PROCESSING";

    private final UserRepository userRepository;
    private final UserResumeRepository userResumeRepository;
    private final AiAnalysisRecordRepository aiAnalysisRecordRepository;
    private final AiAnalysisAsyncService aiAnalysisAsyncService;

    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    @Value("${aws.s3.region}")
    private String region;

    @Transactional
    public UUID saveFiles(UUID userId, List<MultipartFile> files, String authorizationHeader) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        validateFiles(files);
        String desiredJobRole = resolveDesiredJobRole(user);

        List<String> presignedFileUrls = new ArrayList<>();
        List<Integer> savedResumeIds = new ArrayList<>();

        for (MultipartFile file : files) {
            String originalFilename = file.getOriginalFilename();
            UploadedFile uploadedFile = uploadToS3(userId, file);
            presignedFileUrls.add(createPresignedUrl(uploadedFile.key()));

            UserResume userResume = UserResume.builder()
                    .userId(userId)
                    .fileUrl(uploadedFile.fileUrl())
                    .fileName(originalFilename)
                    .build();

            UserResume savedResume = userResumeRepository.save(userResume);
            savedResumeIds.add(savedResume.getResumeId());
        }

        AiAnalysisRecord analysisRecord = AiAnalysisRecord.builder()
                .userId(userId)
                .analysisType("RESUME")
                .resumeId(getRepresentativeResumeId(savedResumeIds))
                .inputSummary(createAnalysisLabel(desiredJobRole, "RESUME"))
                .status(STATUS_PROCESSING)
                .result(PROCESSING_RESULT)
                .createdAt(OffsetDateTime.now())
                .build();

        UUID analysisId = aiAnalysisRecordRepository.save(analysisRecord).getRecordId();

        scheduleAiAnalysisAfterCommit(
                analysisId,
                authorizationHeader,
                desiredJobRole,
                List.copyOf(presignedFileUrls)
        );

        return analysisId;
    }

    private Integer getRepresentativeResumeId(List<Integer> resumeIds) {
        if (resumeIds.isEmpty()) {
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }

        return resumeIds.get(0);
    }

    @Transactional(readOnly = true)
    public ResumeListResponse getMyResumes(UUID userId) {
        userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        List<ResumeResponse> resumes = userResumeRepository.findByUserId(userId)
                .stream()
                .map(resume -> new ResumeResponse(
                        resume.getResumeId(),
                        resume.getFileUrl(),
                        resume.getPortfolioUrl(),
                        resume.getDescription()
                ))
                .toList();

        return new ResumeListResponse(resumes);
    }

    private UploadedFile uploadToS3(UUID userId, MultipartFile file) {
        try (S3Client s3Client = S3Client.builder()
                .region(Region.of(region))
                .build()) {

            String originalFilename = file.getOriginalFilename();
            String extension = getExtension(originalFilename);
            String key = "resumes/" + userId + "/" + UUID.randomUUID() + "." + extension;

            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .contentType(file.getContentType())
                    .contentLength(file.getSize())
                    .build();

            s3Client.putObject(
                    putObjectRequest,
                    RequestBody.fromInputStream(file.getInputStream(), file.getSize())
            );

            String fileUrl = "https://" + bucketName + ".s3." + region + ".amazonaws.com/" + key;

            return new UploadedFile(key, fileUrl);

        } catch (IOException e) {
            throw new RuntimeException("File upload failed.", e);
        }
    }

    private void validateFiles(List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            throw new IllegalArgumentException("At least one file is required.");
        }

        for (MultipartFile file : files) {
            validateFile(file);
        }
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty() || file.getSize() <= 0) {
            throw new IllegalArgumentException("Empty files cannot be uploaded.");
        }

        String originalFilename = file.getOriginalFilename();

        if (originalFilename == null || originalFilename.isBlank()) {
            throw new IllegalArgumentException("Filename is required.");
        }

        String extension = getExtension(originalFilename);

        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException("Unsupported file type.");
        }
    }

    private String getExtension(String filename) {
        int dotIndex = filename.lastIndexOf(".");

        if (dotIndex == -1 || dotIndex == filename.length() - 1) {
            return "";
        }

        return filename.substring(dotIndex + 1).toLowerCase();
    }

    private void scheduleAiAnalysisAfterCommit(
            UUID analysisId,
            String authorizationHeader,
            String desiredJobRole,
            List<String> fileUrls
    ) {
        Runnable task = () -> aiAnalysisAsyncService.requestAndSaveAnalysis(
                analysisId,
                authorizationHeader,
                desiredJobRole,
                fileUrls
        );

        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            task.run();
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                task.run();
            }
        });
    }

    private String resolveDesiredJobRole(User user) {
        String[] desiredJobs = user.getDesiredJobs();

        if (desiredJobs == null) {
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }

        return Arrays.stream(desiredJobs)
                .filter(StringUtils::hasText)
                .findFirst()
                .orElseThrow(() -> new CustomException(ErrorCode.INVALID_REQUEST));
    }

    private String createAnalysisLabel(String desiredJobRole, String analysisType) {
        return desiredJobRole + " " + toAnalysisTypeLabel(analysisType) + " \uBD84\uC11D";
    }

    private String toAnalysisTypeLabel(String analysisType) {
        if ("RESUME".equals(analysisType)) {
            return "\uC774\uB825\uC11C";
        }

        return analysisType;
    }

    private record UploadedFile(
            String key,
            String fileUrl
    ) {
    }

    private String createPresignedUrl(String key) {
        try (S3Presigner presigner = S3Presigner.builder()
                .region(Region.of(region))
                .build()) {

            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();

            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                    .signatureDuration(Duration.ofMinutes(30))
                    .getObjectRequest(getObjectRequest)
                    .build();

            return presigner.presignGetObject(presignRequest).url().toString();
        }
    }
}
