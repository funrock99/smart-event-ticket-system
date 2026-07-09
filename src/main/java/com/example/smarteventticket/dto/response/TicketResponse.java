package com.example.smarteventticket.dto.response;

import com.example.smarteventticket.enums.EventType;
import com.example.smarteventticket.enums.TicketPriority;
import com.example.smarteventticket.enums.TicketStatus;
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
        LocalDateTime closedAt,
        LocalDateTime slaDueAt,
        boolean slaBreached
) {
}
