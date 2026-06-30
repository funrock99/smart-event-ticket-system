package com.example.smartmaintenance.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.example.smartmaintenance.dto.response.DashboardSummaryResponse;
import com.example.smartmaintenance.enums.AlarmSeverity;
import com.example.smartmaintenance.enums.EquipmentStatus;
import com.example.smartmaintenance.enums.TicketStatus;
import com.example.smartmaintenance.repository.AlarmEventRepository;
import com.example.smartmaintenance.repository.EquipmentRepository;
import com.example.smartmaintenance.repository.MaintenanceTicketRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock
    private EquipmentRepository equipmentRepository;

    @Mock
    private MaintenanceTicketRepository ticketRepository;

    @Mock
    private AlarmEventRepository alarmEventRepository;

    @Mock
    private CacheService cacheService;

    @InjectMocks
    private DashboardService dashboardService;

    @Test
    void getSummaryShouldAggregateCounts() {
        when(cacheService.getDashboardSummary()).thenReturn(Optional.empty());
        when(equipmentRepository.count()).thenReturn(12L);
        when(equipmentRepository.countByStatus(EquipmentStatus.RUNNING)).thenReturn(9L);
        when(equipmentRepository.countByStatus(EquipmentStatus.DOWN)).thenReturn(2L);
        when(equipmentRepository.countByStatus(EquipmentStatus.MAINTENANCE)).thenReturn(1L);
        when(ticketRepository.countByStatus(TicketStatus.OPEN)).thenReturn(5L);
        when(ticketRepository.countByStatus(TicketStatus.IN_PROGRESS)).thenReturn(2L);
        when(alarmEventRepository.countBySeverityIn(List.of(AlarmSeverity.HIGH, AlarmSeverity.CRITICAL))).thenReturn(2L);

        var response = dashboardService.getSummary();

        assertEquals(12L, response.totalEquipments());
        assertEquals(5L, response.openTickets());
        assertEquals(2L, response.highSeverityAlarms());
        verify(cacheService).cacheDashboardSummary(response);
    }

    @Test
    void getSummaryShouldReturnCachedValueWhenAvailable() {
        DashboardSummaryResponse cachedResponse = new DashboardSummaryResponse(12, 9, 2, 1, 5, 2, 2);
        when(cacheService.getDashboardSummary()).thenReturn(Optional.of(cachedResponse));

        var response = dashboardService.getSummary();

        assertEquals(cachedResponse, response);
        verifyNoInteractions(equipmentRepository, ticketRepository, alarmEventRepository);
    }
}
