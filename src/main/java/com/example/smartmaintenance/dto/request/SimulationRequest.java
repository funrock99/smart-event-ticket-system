package com.example.smartmaintenance.dto.request;

import com.example.smartmaintenance.enums.AlarmSeverity;
import com.example.smartmaintenance.enums.EventType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SimulationRequest(
        @NotBlank @Size(max = 50) String source,
        @NotNull EventType eventType,
        @NotNull AlarmSeverity severity,
        @NotBlank @Size(max = 100) String businessKeyPrefix,
        @NotBlank @Size(max = 500) String message,
        @Size(max = 5000) String payload,
        @Min(1) @Max(1000) int count,
        boolean duplicateBusinessKey
) {
}