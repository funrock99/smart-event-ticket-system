package com.example.smartmaintenance.dto.request;

import com.example.smartmaintenance.enums.AlarmSeverity;
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

