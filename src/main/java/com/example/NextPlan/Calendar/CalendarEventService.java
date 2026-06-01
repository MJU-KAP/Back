package com.example.NextPlan.Calendar;

import com.example.NextPlan.Entity.CalendarEvent;
import com.example.NextPlan.Entity.Purpose;
import com.example.NextPlan.Kakao.common.CustomException;
import com.example.NextPlan.Kakao.common.ErrorCode;
import com.example.NextPlan.Repository.CalendarEventRepository;
import com.example.NextPlan.Repository.PurposeRepository;
import com.example.NextPlan.Repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CalendarEventService {

    private final CalendarEventRepository calendarEventRepository;
    private final UserRepository userRepository;
    private final PurposeRepository purposeRepository;

    @Transactional
    public CalendarEventResponse createCalendarEvent(UUID userId, CalendarEventRequest request) {
        validateUser(userId);
        validatePurposeOwner(userId, request.purposeId());

        CalendarEvent calendarEvent = CalendarEvent.builder()
                .userId(userId)
                .purposeId(request.purposeId())
                .eventDate(request.eventDate())
                .title(request.title())
                .description(request.description())
                .link(request.link())
                .createdAt(OffsetDateTime.now())
                .build();

        return CalendarEventResponse.from(calendarEventRepository.save(calendarEvent));
    }

    @Transactional(readOnly = true)
    public CalendarEventListResponse getCalendarEvents(UUID userId) {
        validateUser(userId);

        List<CalendarEventResponse> items = calendarEventRepository.findByUserIdOrderByEventDateAscCalendarIdDesc(userId)
                .stream()
                .map(CalendarEventResponse::from)
                .toList();

        return new CalendarEventListResponse(items.size(), items);
    }

    private void validateUser(UUID userId) {
        if (!userRepository.existsById(userId)) {
            throw new CustomException(ErrorCode.USER_NOT_FOUND);
        }
    }

    private void validatePurposeOwner(UUID userId, Integer purposeId) {
        Purpose purpose = purposeRepository.findById(purposeId)
                .orElseThrow(() -> new CustomException(ErrorCode.FORBIDDEN));

        if (!purpose.getUserId().equals(userId)) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }
    }

    public record CalendarEventRequest(
            Integer purposeId,
            LocalDate eventDate,
            String title,
            String description,
            String link
    ) {
    }

    public record CalendarEventResponse(
            Integer calendarId,
            Integer purposeId,
            LocalDate eventDate,
            String title,
            String description,
            String link,
            OffsetDateTime createdAt
    ) {
        public static CalendarEventResponse from(CalendarEvent calendarEvent) {
            return new CalendarEventResponse(
                    calendarEvent.getCalendarId(),
                    calendarEvent.getPurposeId(),
                    calendarEvent.getEventDate(),
                    calendarEvent.getTitle(),
                    calendarEvent.getDescription(),
                    calendarEvent.getLink(),
                    calendarEvent.getCreatedAt()
            );
        }
    }

    public record CalendarEventListResponse(
            int count,
            List<CalendarEventResponse> items
    ) {
    }
}
