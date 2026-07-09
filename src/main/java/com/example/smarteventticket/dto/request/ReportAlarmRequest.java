package com.example.smarteventticket.dto.request;

import com.example.smarteventticket.enums.AlarmSeverity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ReportAlarmRequest(
        @NotBlank String equipmentId,
        @NotBlank String alarmCode,
        @NotNull AlarmSeverity severity,
        @NotBlank @Size(max = 500) String message
) {
}


