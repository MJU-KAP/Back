package com.example.NextPlan.Calendar;

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
@RequestMapping("/api/calendar")
@RequiredArgsConstructor
public class CalendarEventController {

    private final CalendarEventService calendarEventService;

    @PostMapping
    public ResponseEntity<CalendarEventService.CalendarEventResponse> createCalendarEvent(
            Authentication authentication,
            @Valid @RequestBody CalendarEventCreateRequest request
    ) {
        UUID userId = UUID.fromString(authentication.getName());

        CalendarEventService.CalendarEventResponse response = calendarEventService.createCalendarEvent(
                userId,
                request.toServiceRequest()
        );

        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<CalendarEventService.CalendarEventListResponse> getCalendarEvents(
            Authentication authentication
    ) {
        UUID userId = UUID.fromString(authentication.getName());
        return ResponseEntity.ok(calendarEventService.getCalendarEvents(userId));
    }

    public record CalendarEventCreateRequest(
            @NotNull(message = "eventDate is required")
            LocalDate eventDate,

            @NotBlank(message = "title is required")
            String title,

            String description,
            String link
    ) {
        public CalendarEventService.CalendarEventRequest toServiceRequest() {
            return new CalendarEventService.CalendarEventRequest(eventDate, title, description, link);
        }
    }
}
