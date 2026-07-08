package com.example.smartmaintenance.dto.request;

import com.example.smartmaintenance.enums.AlarmSeverity;
import com.example.smartmaintenance.enums.EventType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateEventRequest(
        @NotBlank @Size(max = 50) String source,
        @NotNull EventType eventType,
        @NotBlank @Size(max = 100) String businessKey,
        @NotNull AlarmSeverity severity,
        @NotBlank @Size(max = 500) String message,
        @Size(max = 5000) String payload
) {
}