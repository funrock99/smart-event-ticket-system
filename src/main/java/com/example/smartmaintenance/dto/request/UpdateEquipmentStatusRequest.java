package com.example.smartmaintenance.dto.request;

import com.example.smartmaintenance.enums.EquipmentStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateEquipmentStatusRequest(
        @NotNull EquipmentStatus status
) {
}

