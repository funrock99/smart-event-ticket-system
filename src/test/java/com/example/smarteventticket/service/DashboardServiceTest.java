package com.example.smarteventticket.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.example.smarteventticket.dto.response.DashboardSummaryResponse;
import com.example.smarteventticket.enums.TicketStatus;
import com.example.smarteventticket.repository.AlarmEventRepository;
import com.example.smarteventticket.repository.MaintenanceTicketRepository;
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
        when(cacheService.getIdempotentReplayCount()).thenReturn(2L);
        when(ticketRepository.countByStatus(TicketStatus.OPEN)).thenReturn(5L);
        when(ticketRepository.countByStatus(TicketStatus.PROCESSING)).thenReturn(2L);
        when(ticketRepository.countByStatus(TicketStatus.RESOLVED)).thenReturn(1L);
        when(ticketRepository.countByStatus(TicketStatus.CLOSED)).thenReturn(4L);

        var response = dashboardService.getSummary();

        assertEquals(25L, response.totalEvents());
        assertEquals(12L, response.validEvents());
        assertEquals(8L, response.duplicatedEvents());
        assertEquals(3L, response.rateLimitedEvents());
        assertEquals(2L, response.idempotentReplayedEvents());
        verify(cacheService).cacheDashboardSummary(response);
    }

    @Test
    void getSummaryShouldReturnCachedValueWhenAvailable() {
        DashboardSummaryResponse cachedResponse = new DashboardSummaryResponse(25L, 12L, 8L, 3L, 2L, 5L, 2L, 1L, 4L);
        when(cacheService.getDashboardSummary()).thenReturn(Optional.of(cachedResponse));

        var response = dashboardService.getSummary();

        assertEquals(cachedResponse, response);
        verifyNoInteractions(ticketRepository, alarmEventRepository);
    }
}
