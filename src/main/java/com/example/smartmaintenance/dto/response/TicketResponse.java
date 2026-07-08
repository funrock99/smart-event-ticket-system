package com.example.smartmaintenance.dto.response;

import com.example.smartmaintenance.enums.EventType;
import com.example.smartmaintenance.enums.TicketPriority;
import com.example.smartmaintenance.enums.TicketStatus;
import java.time.LocalDateTime;

public record TicketResponse(
        Long id,
        String ticketNo,
        String source,
        EventType eventType,
        String businessKey,
        TicketPriority priority,
        TicketStatus status,
        String assignee,
        String description,
        LocalDateTime createdAt,
        LocalDateTime resolvedAt,
        LocalDateTime closedAt
) {
}