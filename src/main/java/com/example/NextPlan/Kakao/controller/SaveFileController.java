package com.example.NextPlan.Kakao.controller;

import com.example.NextPlan.Kakao.Service.SaveFileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/fileupload")
@RequiredArgsConstructor
public class SaveFileController {

    private final SaveFileService saveFileService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<FileUploadResponse> saveFiles(
            Authentication authentication,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader,
            @RequestPart("files") List<MultipartFile> files
    ) {
        UUID userId = UUID.fromString(authentication.getName());

        UUID analysisId = saveFileService.saveFiles(userId, files, authorizationHeader);

        return ResponseEntity.ok(new FileUploadResponse(analysisId));
    }

    public record FileUploadResponse(
            UUID analysisId
    ) {
    }
}
