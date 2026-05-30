package com.example.NextPlan.Repository;

import com.example.NextPlan.Entity.CalendarEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CalendarEventRepository extends JpaRepository<CalendarEvent, Integer> {

    List<CalendarEvent> findByUserIdOrderByEventDateAscCalendarIdDesc(UUID userId);
}
