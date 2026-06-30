package com.example.smartmaintenance.dto.response;

public record DashboardSummaryResponse(
        long totalEquipments,
        long runningEquipments,
        long downEquipments,
        long maintenanceEquipments,
        long openTickets,
        long inProgressTickets,
        long highSeverityAlarms
) {
}

