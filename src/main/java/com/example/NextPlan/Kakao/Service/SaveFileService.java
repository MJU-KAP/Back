package com.example.NextPlan.Kakao.Service;

import com.example.NextPlan.Entity.UserResume;
import com.example.NextPlan.Kakao.common.CustomException;
import com.example.NextPlan.Kakao.common.ErrorCode;
import com.example.NextPlan.Kakao.controller.ReturnResumeController.ResumeListResponse;
import com.example.NextPlan.Kakao.controller.ReturnResumeController.ResumeResponse;
import com.example.NextPlan.Repository.UserRepository;
import com.example.NextPlan.Repository.UserResumeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SaveFileService {

    private final UserRepository userRepository;
    private final UserResumeRepository userResumeRepository;

    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    @Value("${aws.s3.region}")
    private String region;

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            "pdf", "docx", "txt", "png", "jpg", "jpeg"
    );

    @Transactional
    public void saveFiles(UUID userId, List<MultipartFile> files) {
        userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.FORBIDDEN));

        validateFiles(files);

        for (MultipartFile file : files) {
            String originalFilename = file.getOriginalFilename();
            String fileUrl = uploadToS3(userId, file);

            UserResume userResume = UserResume.builder()
                    .userId(userId)
                    .fileUrl(fileUrl)
                    .fileName(originalFilename)
                    .build();

            userResumeRepository.save(userResume);
        }
    }

    @Transactional(readOnly = true)
    public ResumeListResponse getMyResumes(UUID userId) {
        userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.FORBIDDEN));

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

    private String uploadToS3(UUID userId, MultipartFile file) {
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

            return "https://" + bucketName + ".s3." + region + ".amazonaws.com/" + key;

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
        if (file == null || file.isEmpty()) {
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
}
