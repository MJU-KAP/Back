package com.example.NextPlan.Purpose;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/purposes")
@RequiredArgsConstructor
public class PurposeController {

    private final PurposeService purposeService;

    @PostMapping
    public ResponseEntity<PurposeService.PurposeResponse> createPurpose(
            Authentication authentication,
            @Valid @RequestBody PurposeCreateRequest request
    ) {
        UUID userId = UUID.fromString(authentication.getName());

        PurposeService.PurposeResponse response = purposeService.createPurpose(
                userId,
                request.toServiceRequest()
        );

        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<PurposeService.PurposeListResponse> getPurposes(
            Authentication authentication
    ) {
        UUID userId = UUID.fromString(authentication.getName());
        return ResponseEntity.ok(purposeService.getPurposes(userId));
    }

    public record PurposeCreateRequest(
            @NotBlank(message = "name is required")
            String name,

            @NotNull(message = "date is required")
            LocalDate date,

            String link,
            String type,
            String goal
    ) {
        public PurposeService.PurposeRequest toServiceRequest() {
            return new PurposeService.PurposeRequest(name, date, link, type, goal);
        }
    }
}
