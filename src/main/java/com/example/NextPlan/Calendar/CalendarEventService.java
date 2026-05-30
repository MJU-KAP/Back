package com.example.NextPlan.Calendar;

import com.example.NextPlan.Entity.CalendarEvent;
import com.example.NextPlan.Kakao.common.CustomException;
import com.example.NextPlan.Kakao.common.ErrorCode;
import com.example.NextPlan.Repository.CalendarEventRepository;
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

    @Transactional
    public CalendarEventResponse createCalendarEvent(UUID userId, CalendarEventRequest request) {
        validateUser(userId);

        CalendarEvent calendarEvent = CalendarEvent.builder()
                .userId(userId)
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

    public record CalendarEventRequest(
            LocalDate eventDate,
            String title,
            String description,
            String link
    ) {
    }

    public record CalendarEventResponse(
            Integer calendarId,
            LocalDate eventDate,
            String title,
            String description,
            String link,
            OffsetDateTime createdAt
    ) {
        public static CalendarEventResponse from(CalendarEvent calendarEvent) {
            return new CalendarEventResponse(
                    calendarEvent.getCalendarId(),
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
