package com.example.smarteventticket.dto.response;

import com.example.smarteventticket.enums.EquipmentStatus;
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


