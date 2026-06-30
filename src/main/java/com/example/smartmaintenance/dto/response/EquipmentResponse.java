package com.example.smartmaintenance.dto.response;

import com.example.smartmaintenance.enums.EquipmentStatus;
import java.time.LocalDateTime;

public record EquipmentResponse(
        Long id,
        String equipmentId,
        String name,
        String factoryArea,
        EquipmentStatus status,
        LocalDateTime lastHeartbeatTime
) {
}

