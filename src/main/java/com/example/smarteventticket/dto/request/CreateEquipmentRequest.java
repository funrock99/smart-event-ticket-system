package com.example.smarteventticket.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateEquipmentRequest(
        @NotBlank @Size(min = 3, max = 30) String equipmentId,
        @NotBlank @Size(min = 1, max = 100) String name,
        @NotBlank @Size(min = 1, max = 50) String factoryArea
) {
}


