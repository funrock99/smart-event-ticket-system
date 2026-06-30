package com.example.smartmaintenance.service;

import com.example.smartmaintenance.dto.response.DashboardSummaryResponse;
import com.example.smartmaintenance.enums.AlarmSeverity;
import com.example.smartmaintenance.enums.EquipmentStatus;
import com.example.smartmaintenance.enums.TicketStatus;
import com.example.smartmaintenance.repository.AlarmEventRepository;
import com.example.smartmaintenance.repository.EquipmentRepository;
import com.example.smartmaintenance.repository.MaintenanceTicketRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DashboardService {

    private final EquipmentRepository equipmentRepository;
    private final MaintenanceTicketRepository ticketRepository;
    private final AlarmEventRepository alarmEventRepository;
    private final CacheService cacheService;

    public DashboardService(
            EquipmentRepository equipmentRepository,
            MaintenanceTicketRepository ticketRepository,
            AlarmEventRepository alarmEventRepository,
            CacheService cacheService
    ) {
        this.equipmentRepository = equipmentRepository;
        this.ticketRepository = ticketRepository;
        this.alarmEventRepository = alarmEventRepository;
        this.cacheService = cacheService;
    }

    @Transactional(readOnly = true)
    public DashboardSummaryResponse getSummary() {
        return cacheService.getDashboardSummary().orElseGet(() -> {
            DashboardSummaryResponse response = new DashboardSummaryResponse(
                    equipmentRepository.count(),
                    equipmentRepository.countByStatus(EquipmentStatus.RUNNING),
                    equipmentRepository.countByStatus(EquipmentStatus.DOWN),
                    equipmentRepository.countByStatus(EquipmentStatus.MAINTENANCE),
                    ticketRepository.countByStatus(TicketStatus.OPEN),
                    ticketRepository.countByStatus(TicketStatus.IN_PROGRESS),
                    alarmEventRepository.countBySeverityIn(List.of(AlarmSeverity.HIGH, AlarmSeverity.CRITICAL))
            );
            cacheService.cacheDashboardSummary(response);
            return response;
        });
    }
}
