package com.example.smartmaintenance.service;

import com.example.smartmaintenance.dto.response.DashboardSummaryResponse;
import com.example.smartmaintenance.enums.TicketStatus;
import com.example.smartmaintenance.repository.AlarmEventRepository;
import com.example.smartmaintenance.repository.MaintenanceTicketRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DashboardService {

    private final MaintenanceTicketRepository ticketRepository;
    private final AlarmEventRepository alarmEventRepository;
    private final CacheService cacheService;

    public DashboardService(
            MaintenanceTicketRepository ticketRepository,
            AlarmEventRepository alarmEventRepository,
            CacheService cacheService
    ) {
        this.ticketRepository = ticketRepository;
        this.alarmEventRepository = alarmEventRepository;
        this.cacheService = cacheService;
    }

    @Transactional(readOnly = true)
    public DashboardSummaryResponse getSummary() {
        return cacheService.getDashboardSummary().orElseGet(() -> {
            long validEvents = alarmEventRepository.count();
            long duplicatedEvents = cacheService.getDuplicateEventCount();
            long rateLimitedEvents = cacheService.getRateLimitedEventCount();
            DashboardSummaryResponse response = new DashboardSummaryResponse(
                    validEvents + duplicatedEvents + rateLimitedEvents,
                    validEvents,
                    duplicatedEvents,
                    rateLimitedEvents,
                    ticketRepository.countByStatus(TicketStatus.OPEN),
                    ticketRepository.countByStatus(TicketStatus.PROCESSING),
                    ticketRepository.countByStatus(TicketStatus.RESOLVED),
                    ticketRepository.countByStatus(TicketStatus.CLOSED)
            );
            cacheService.cacheDashboardSummary(response);
            return response;
        });
    }
}