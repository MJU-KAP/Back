package com.example.NextPlan.auth.controller;

import com.example.NextPlan.auth.Service.UserPreferenceService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
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
                request.desiredJobRole(),
                request.techStacks()
        );
        return ResponseEntity.ok(Map.of("message", "success"));
    }

    public record PreferenceRequest(
            @NotBlank(message = "desiredJobRole is required")
            String desiredJobRole,

            @NotEmpty(message = "techStacks is required")
            @Size(max = 15, message = "techStacks must be less than or equal to 15")
            List<@NotBlank(message = "techStack must not be blank") String> techStacks
    ) {
    }
}
