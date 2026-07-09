package com.example.smarteventticket.dto.request;

import com.example.smarteventticket.enums.EquipmentStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateEquipmentStatusRequest(
        @NotNull EquipmentStatus status
) {
}


