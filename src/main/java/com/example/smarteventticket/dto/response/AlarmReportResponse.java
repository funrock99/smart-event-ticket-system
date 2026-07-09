package com.example.smarteventticket.dto.response;

import com.example.smarteventticket.enums.AlarmSeverity;

public record AlarmReportResponse(
        Long alarmId,
        String equipmentId,
        String alarmCode,
        AlarmSeverity severity,
        String ticketNo,
        String message
) {
}


