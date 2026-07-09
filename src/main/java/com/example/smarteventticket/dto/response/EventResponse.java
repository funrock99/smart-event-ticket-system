package com.example.smarteventticket.dto.response;

import com.example.smarteventticket.enums.AlarmSeverity;
import com.example.smarteventticket.enums.EventType;
import java.time.LocalDateTime;

public record EventResponse(
        Long id,
        String source,
        EventType eventType,
        String businessKey,
        AlarmSeverity severity,
        String message,
        String payload,
        LocalDateTime occurredAt,
        LocalDateTime createdAt
) {
}
