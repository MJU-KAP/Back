package com.example.NextPlan.Kakao.controller;

import com.example.NextPlan.Kakao.Service.SaveFileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/users/me/resumes")
@RequiredArgsConstructor
public class ReturnResumeController {

    private final SaveFileService saveFileService;

    @GetMapping
    public ResponseEntity<ResumeListResponse> getMyResumes(
            Authentication authentication
    ) {
        UUID userId = UUID.fromString(authentication.getName());

        ResumeListResponse response = saveFileService.getMyResumes(userId);

        return ResponseEntity.ok(response);
    }

    public record ResumeListResponse(
            List<ResumeResponse> resumes
    ) {
    }

    public record ResumeResponse(
            Integer resumeId,
            String fileUrl,
            String portfolioUrl,
            String description
    ) {
    }
}
