package com.example.smartmaintenance.dto.response;

import com.example.smartmaintenance.enums.AlarmSeverity;
import com.example.smartmaintenance.enums.EventType;
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