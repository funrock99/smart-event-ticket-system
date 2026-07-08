package com.example.smartmaintenance.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.example.smartmaintenance.dto.response.DashboardSummaryResponse;
import com.example.smartmaintenance.enums.TicketStatus;
import com.example.smartmaintenance.repository.AlarmEventRepository;
import com.example.smartmaintenance.repository.MaintenanceTicketRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

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
        when(alarmEventRepository.count()).thenReturn(12L);
        when(cacheService.getDuplicateEventCount()).thenReturn(8L);
        when(cacheService.getRateLimitedEventCount()).thenReturn(3L);
        when(ticketRepository.countByStatus(TicketStatus.OPEN)).thenReturn(5L);
        when(ticketRepository.countByStatus(TicketStatus.PROCESSING)).thenReturn(2L);
        when(ticketRepository.countByStatus(TicketStatus.RESOLVED)).thenReturn(1L);
        when(ticketRepository.countByStatus(TicketStatus.CLOSED)).thenReturn(4L);

        var response = dashboardService.getSummary();

        assertEquals(23L, response.totalEvents());
        assertEquals(12L, response.validEvents());
        assertEquals(8L, response.duplicatedEvents());
        assertEquals(3L, response.rateLimitedEvents());
        verify(cacheService).cacheDashboardSummary(response);
    }

    @Test
    void getSummaryShouldReturnCachedValueWhenAvailable() {
        DashboardSummaryResponse cachedResponse = new DashboardSummaryResponse(23, 12, 8, 3, 5, 2, 1, 4);
        when(cacheService.getDashboardSummary()).thenReturn(Optional.of(cachedResponse));

        var response = dashboardService.getSummary();

        assertEquals(cachedResponse, response);
        verifyNoInteractions(ticketRepository, alarmEventRepository);
    }
}