package com.example.smartmaintenance.dto.response;

import com.example.smartmaintenance.enums.AlarmSeverity;

public record AlarmReportResponse(
        Long alarmId,
        String equipmentId,
        String alarmCode,
        AlarmSeverity severity,
        String ticketNo,
        String message
) {
}

