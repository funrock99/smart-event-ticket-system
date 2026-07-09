package com.example.smarteventticket.dto.response;

import com.example.smarteventticket.enums.AlarmSeverity;
import java.time.LocalDateTime;

public record AlarmEventResponse(
        Long id,
        String equipmentId,
        String alarmCode,
        AlarmSeverity severity,
        String message,
        LocalDateTime occurredAt,
        LocalDateTime createdAt
) {
}


