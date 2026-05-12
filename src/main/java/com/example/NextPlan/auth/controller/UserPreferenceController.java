package com.example.NextPlan.auth.controller;

import com.example.NextPlan.auth.Service.UserPreferenceService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/users/me/preferences")
@RequiredArgsConstructor
public class UserPreferenceController {

    private final UserPreferenceService userPreferenceService;

    @PostMapping
    public ResponseEntity<?> savePreferences(
            Authentication authentication,
            @Valid @RequestBody PreferenceRequest request
    ) {
        UUID userId = UUID.fromString(authentication.getName());

        userPreferenceService.savePreference(
                userId,
                request.desiredJobs(),
                request.skills()
        );
        return ResponseEntity.ok(Map.of("message", "success"));
    }

    public record PreferenceRequest(
            @NotEmpty(message = "desiredJobs is required")
            List<String> desiredJobs,

            @NotEmpty(message = "skills is required")
            List<String> skills
    ) {
    }
}
