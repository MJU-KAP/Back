package com.example.NextPlan.Kakao.controller;

import com.example.NextPlan.Kakao.Service.SaveFileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/analyze")
@RequiredArgsConstructor
public class SaveFileController {

    private final SaveFileService saveFileService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Void> saveFiles(
            Authentication authentication,
            @RequestPart("files") List<MultipartFile> files
    ) {
        UUID userId = UUID.fromString(authentication.getName());

        saveFileService.saveFiles(userId, files);

        return ResponseEntity.ok().build();
    }
}
